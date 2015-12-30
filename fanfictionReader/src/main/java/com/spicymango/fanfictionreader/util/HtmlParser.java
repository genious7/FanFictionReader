package com.spicymango.fanfictionreader.util;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AlignmentSpan;
import android.text.style.LineBackgroundSpan;
import android.text.style.QuoteSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.w3c.dom.Text;

import java.util.List;
import java.util.Stack;

/**
 * This class processes HTML strings into displayable styled text. This class supports additional
 * tags over the default android.text.Html class. Note that the android.text.Html.TagHandler cannot
 * be used since the attributes are not passed to the handler; therefore, the text-align attribute
 * cannot be processed.
 * <p/>
 * Created by Michael Chen on 12/28/2015.
 */
public class HtmlParser {

	/**
	 * Returns displayable styled text from the provided HTML string. The base uri is used to
	 * resolve relative URLs into absolute URLs
	 *
	 * @param source  The source Html web page, as a string
	 * @param baseUri The base uri
	 * @return The document as styled text
	 */
	public static Spanned fromHtml(String source) {
		Document htmlDoc = Jsoup.parse(source, Sites.FANFICTION.BASE_URI.toString());
		return fromHtml(htmlDoc);
	}

	/**
	 * Returns displayable styled text from the provided HTML string. The base uri is used to
	 * resolve relative URLs into absolute URLs
	 *
	 * @param source  The source Html web page, as a string
	 * @param baseUri The base uri
	 * @return The document as styled text
	 */
	public static Spanned fromHtml(String source, Uri baseUri) {
		Document htmlDoc = Jsoup.parse(source, baseUri.toString());
		return fromHtml(htmlDoc);
	}

	/**
	 * Returns displayable styled text from the provided HTML element.
	 *
	 * @param source The source Html web page, as an element
	 * @return The document as styled text
	 */
	public static Spanned fromHtml(Element source) {
		HtmlToSpannedConverter converter = new HtmlToSpannedConverter(source);
		return converter.convert();
	}

	/**
	 * Converts a jsoup html element into stylized text
	 */
	private static final class HtmlToSpannedConverter {
		/**
		 * The source element
		 */
		private final Element mSource;

		private final SpannableStringBuilder mBuilder;

		/**
		 * A stack used to remember the starting position of each span
		 */
		private final Stack<Integer> mSpanPositions;

		/**
		 * Creates a new HtmlToSpannedConverter
		 *
		 * @param source The source element
		 */
		public HtmlToSpannedConverter(Element source) {
			mBuilder = new SpannableStringBuilder();
			mSpanPositions = new Stack<>();

			// If the provided source is the HTML doc, parse only the body.
			if (source instanceof Document)
				mSource = ((Document) source).body();
			else {
				mSource = source;
			}
		}

		/**
		 * Converts the provided html element into a stylized string
		 *
		 * @return A spanned string
		 */
		public Spanned convert() {
			convert(mSource);
			return mBuilder;
		}

		/**
		 * A recursive function that converts an element and its children, creating spans as
		 * required.
		 *
		 * @param element The element to convert
		 */
		public void convert(Element element) {
			// Begin the span
			handleStartTag(element);

			// Process the intermediate nodes
			List<Node> nodes = element.childNodes();

			for (Node node : nodes) {
				if (node instanceof Element) {
					// Recursively convert element nodes
					convert((Element) node);
				} else if (node instanceof TextNode) {
					// Add the text to the span
					characters(((TextNode) node).getWholeText());
				}
			}

			// End the span
			handleEndTag(element);
		}

		/**
		 * Handles any css code that is inlined in the document
		 *
		 * @param css
		 */
		private void handleCssStart(String css) {
			String rules[] = css.split(";");
			for (String rule : rules) {
				String tmp[] = rule.split(":");
				if (tmp.length < 2) continue;
				String key = tmp[0];
				String value = tmp[1];

				switch (key.toLowerCase()) {
					case "text-align":
						startSpan();
						break;
				}
			}
		}

		/**
		 * Handles any css code that is inlined in the document
		 *
		 * @param css
		 */
		private void handleCssEnd(String css) {
			String rules[] = css.split(";");
			for (String rule : rules) {
				String tmp[] = rule.split(":");
				if (tmp.length < 2) continue;
				String key = tmp[0];
				String value = tmp[1];

				switch (key.toLowerCase()) {
					case "text-align":
						if (value.equalsIgnoreCase("center")) {
							endSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER));
						} else if (value.equalsIgnoreCase("right")) {
							endSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE));
						} else {
							endSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_NORMAL));
						}
						break;
					case "text-decoration":
						switch(value.toLowerCase()){
							case "underline":
								endSpan(new UnderlineSpan());
								break;
						}
						break;
				}
			}
		}

		/**
		 * Handles the initial tag of every element
		 *
		 * @param element
		 */
		private void handleStartTag(Element element) {
			final String tag = element.tagName();
			switch (tag.toLowerCase()) {
				case "br":
					mBuilder.append('\n');
					break;
				case "hr":
					handleHR();
					break;
				case "p":
				case "div":
					handleP();
					break;
				case "blockquote":
					handleP();
					startSpan();
					break;
				case "b":
				case "strong":
				case "em":
				case "cite":
				case "dfn":
				case "i":
				case "big":
				case "small":
				case "tt":
				case "u":
				case "sub":
				case "sup":
				case "a":
				case "span":
					startSpan();
					break;
			}

			final Attributes attributes = element.attributes();
			if (attributes.hasKey("style"))
				handleCssStart(attributes.get("style"));
		}

		/***
		 * Handles the tailing tag of every element
		 *
		 * @param element
		 */
		private void handleEndTag(Element element) {
			final Attributes attributes = element.attributes();
			if (attributes.hasKey("style"))
				handleCssEnd(attributes.get("style"));

			final String tag = element.tagName();
			switch (tag.toLowerCase()) {
				case "p":
				case "div":
					handleP();
					break;
				case "b":
				case "strong":
					endSpan(new StyleSpan(Typeface.BOLD));
					break;
				case "em":
				case "cite":
				case "dfn":
				case "i":
					endSpan(new StyleSpan(Typeface.ITALIC));
					break;
				case "big":
					endSpan(new RelativeSizeSpan(1.25f));
					break;
				case "small":
					endSpan(new RelativeSizeSpan(0.8f));
					break;
				case "blockquote":
					endSpan(new QuoteSpan());
					handleP();
					break;
				case "tt":
					endSpan(new TypefaceSpan("monospace"));
					break;
				case "u":
					endSpan(new UnderlineSpan());
					break;
				case "sub":
					endSpan(new SubscriptSpan());
					break;
				case "sup":
					endSpan(new SuperscriptSpan());
					break;
				case "a":
					handleLink(element.absUrl("href"));
			}
		}

		/**
		 * Marks the beginning of a span
		 */
		private void startSpan() {
			int position = mBuilder.length();
			mSpanPositions.push(position);
		}

		/**
		 * Closes a span, using the provided span type as a tag
		 *
		 * @param type
		 */
		private void endSpan(Object type) {
			int finalPos = mBuilder.length();
			int initialPos = mSpanPositions.pop();

			// Ignore empty tags
			if (finalPos != initialPos)
				mBuilder.setSpan(type, initialPos, finalPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}

		/**
		 * Adds newline characters as required in order to ensure that each paragraph is separated
		 * from the others.
		 */
		private void handleP() {
			int len = mBuilder.length();

			// If the length is greater than one, ensure that the two preceding characters are
			// newlines, else append one or two newlines, as required.
			if (len >= 1 && mBuilder.charAt(len - 1) == '\n') {
				if (len >= 2 && mBuilder.charAt(len - 2) == '\n') {
					return;
				}
				mBuilder.append("\n");
				return;
			}

			// Note that the double new lines are not required at the beginning of the page.
			if (len != 0) {
				mBuilder.append("\n\n");
			}
		}

		/**
		 * Adds the characters to the output. This function strips extra whitespaces
		 *
		 * @param txt
		 */
		private void characters(String txt) {

			for (int i = 0; i < txt.length(); i++) {
				char c = txt.charAt(i);

				if (c == ' ' || c == '\n') {
					final char previous;

					if (mBuilder.length() == 0) {
						previous = '\n';
					} else {
						previous = mBuilder.charAt(mBuilder.length() - 1);
					}

					if (previous != '\n' && previous != ' ') {
						mBuilder.append(' ');
					}
				} else {
					mBuilder.append(c);
				}
			}
		}

		/**
		 * Inserts a horizontal break line in the page
		 */
		private void handleHR() {

			// If the preceding character is not a newline or the start of the page, add a newline
			int len = mBuilder.length();
			if (len != 0 && mBuilder.charAt(len - 1) != '\n'){
				mBuilder.append('\n');
			}

			// A non-zero length inside the span is required in order for the span to be drawn
			startSpan();
			mBuilder.append(' ');
			endSpan(new HorizontalBreakSpan());

			mBuilder.append('\n');
		}

		/**
		 * If a URL is provided, creates a clickable URLSpan
		 *
		 * @param href
		 */
		private void handleLink(String href) {
			if (TextUtils.isEmpty(href)) {
				// If the link is invalid, don't create a new span. However, the span start value
				// still needs to be removed from the stack.
				mSpanPositions.pop();
			} else {
				endSpan(new URLSpan(href));
			}
		}

		/**
		 * A custom Span that draws a horizontal line across the screen.
		 */
		private static class HorizontalBreakSpan implements LineBackgroundSpan, Parcelable {
			public static final Creator CREATOR = new Creator() {
				@Override
				public Object createFromParcel(Parcel source) {
					return new HorizontalBreakSpan();
				}

				@Override
				public Object[] newArray(int size) {
					return new HorizontalBreakSpan[size];
				}
			};

			@Override
			public void drawBackground(Canvas c, Paint p, int left, int right, int top, int baseline, int bottom, CharSequence text, int start, int end, int lnum) {
				int width = c.getWidth();
				int midPoint = (top + bottom) / 2;
				c.drawLine(0, midPoint, width, midPoint, p);
			}

			@Override
			public int describeContents() {
				return 0;
			}

			@Override
			public void writeToParcel(Parcel dest, int flags) {

			}
		}
	}
}
