package com.spicymango.fanfictionreader.util;

import androidx.annotation.NonNull;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.UncheckedIOException;
import org.jsoup.nodes.Document;

import java.io.IOException;

public class JsoupUtil {
	/**
	 * A helper to use Jsoup get().
	 *
	 * It converts any underlying unchecked {@link UncheckedIOException} to checked {@link IOException}
	 * and throws the checked on instead.
	 *
	 * Use case: application codes have been written to handle and recover from checked IOException.
	 * This helper ensures the recovery will be equally applied to unchecked ones. Otherwise,
	 * the application will crash.
	 *
	 */
	public static Document safeGet(@NonNull String uri, String userAgent) throws IOException {
		try {
			Connection conn = Jsoup.connect(uri).timeout(10000);
			if (userAgent != null) {
				conn.userAgent(userAgent);
			}

			return conn.get();
		} catch (UncheckedIOException uioe) {
			throw uioe.ioException();
		}
	}

    public static Document safeGet(@NonNull String uri) throws IOException {
	    return safeGet(uri, null);
    }

}
