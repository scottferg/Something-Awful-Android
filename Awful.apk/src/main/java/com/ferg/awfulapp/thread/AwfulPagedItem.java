/********************************************************************************
 * Copyright (c) 2011, Scott Ferguson
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the software nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY SCOTT FERGUSON ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL SCOTT FERGUSON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package com.ferg.awfulapp.thread;

import android.util.Log;

import com.ferg.awfulapp.constants.Constants;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.regex.Pattern;

public abstract class AwfulPagedItem {
    private static final String TAG = "AwfulPagedItem";
    
	private static final Pattern pageNumber_regex = Pattern.compile("Pages \\((\\d+)\\)");
	
    public static int parseLastPage(Document pagedItem){
		Elements pages = pagedItem.getElementsByClass("pages");
        return Math.max(getPageNum(pages.first()), getPageNum(pages.last()));
    }

	private static int getPageNum(Element pageElement) {
		try {
			return Integer.parseInt(pageElement.getElementsByTag("option").last().text());
		} catch (NumberFormatException e) {
				Log.e(TAG, "NumberFormatException thrown during parseLastPage()");
				e.printStackTrace();
		} catch (NullPointerException e) { } // not actually exceptional in 1-page threads!
		return 1;
	}
    
	public static int indexToPage(int index, int perPage){
		return (index-1)/perPage+1;//easier than using math.ceil.
	}
	public static int pageToIndex(int page, int perPage, int offset){
		return (page-1)*perPage+1+offset;
	}
	/**
	 * Converts page number to index assuming default item-per-page.
	 * ONLY USE FOR FORUM/THREADS, posts REQUIRE the dynamic per-page setting.
	 * @param page
	 * @return starting index
	 */
	public static int forumPageToIndex(int page) {
		return Math.max(1, (page-1)*Constants.THREADS_PER_PAGE+1);
	}
	

	public static int getLastReadPage(int unread, int total, int postPerPage, int hasReadThread) {
		if(unread<0 || total < 1 || hasReadThread == 0){
			return 1;
		}
		if(unread == 0){
			return indexToPage(total,postPerPage);
		}
		return indexToPage(total-unread+1,postPerPage);
    }
}
