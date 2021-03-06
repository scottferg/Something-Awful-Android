package com.ferg.awfulapp.task

import android.content.Context
import com.ferg.awfulapp.constants.Constants.*
import com.ferg.awfulapp.util.AwfulError
import org.jsoup.nodes.Document

/**
 * An AwfulRequest that adds a userId to the user's ignore list.
 */
class IgnoreRequest(context: Context, userId: Int)
    : AwfulRequest<Void?>(context, FUNCTION_MEMBER2, isPostRequest = true) {
    init {
        with(parameters) {
            add(PARAM_ACTION, ACTION_ADDLIST)
            add(PARAM_USERLIST, USERLIST_IGNORE)
            add(FORMKEY, preferences.ignoreFormkey)
            add(PARAM_USER_ID, Integer.toString(userId))
        }
    }

    @Throws(AwfulError::class)
    override fun handleResponse(doc: Document): Void? = null // nothing to handle, just fire and forget

}
