package com.flipkart.layoutengine.builder;

import android.app.Activity;

import com.flipkart.layoutengine.ParserContext;
import com.flipkart.layoutengine.provider.Provider;
import com.flipkart.layoutengine.view.ProteusView;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A layout builder which can parse view blocks before passing it on to {@link SimpleLayoutBuilder}
 */
public class ViewParsingLayoutBuilder extends SimpleLayoutBuilder {

    private Provider viewProvider;

    public ViewParsingLayoutBuilder(Activity activity, Provider viewProvider) {
        super(activity);
        this.viewProvider = viewProvider;
    }

    @Override
    protected ProteusView onUnknownViewEncountered(ParserContext context, String viewType, ProteusView parent, JsonObject jsonObject, int childIndex) {
        JsonElement jsonElement = viewProvider.getObject(viewType, childIndex);
        if (jsonElement != null) {
            return buildImpl(context, parent, jsonElement.getAsJsonObject(), null, childIndex);
        } else {
            return super.onUnknownViewEncountered(context, viewType, parent, jsonObject, childIndex);
        }
    }
}
