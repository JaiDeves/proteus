/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION
 *
 * Copyright (c) 2017 Flipkart Internet Pvt. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.flipkart.android.proteus;

import android.content.res.XmlResourceParser;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

import com.flipkart.android.proteus.toolbox.Styles;
import com.google.gson.JsonObject;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author kirankumar
 */
public abstract class TypeParser<V extends View> {

    private static XmlResourceParser sParser = null;

    @Nullable
    private TypeParser parent;
    private AttributeProcessor[] processors = new AttributeProcessor[0];

    private Map<String, AttributeSet.Attribute> attributes = new HashMap<>();

    private int offset;
    private AttributeSet attributeSet;


    public void onBeforeCreateView(ProteusLayoutInflater inflater, ViewGroup parent, Layout layout, JsonObject data, Styles styles, int index) {
        // nothing to do here
    }

    public abstract ProteusView createView(ProteusLayoutInflater inflater, ViewGroup parent, Layout layout, JsonObject data, Styles styles, int index);

    public void onAfterCreateView(ProteusLayoutInflater inflater, ViewGroup parent, V view, Layout layout, JsonObject data, Styles styles, int index) {
        if (null == view.getLayoutParams()) {
            ViewGroup.LayoutParams layoutParams = generateDefaultLayoutParams(parent);
            view.setLayoutParams(layoutParams);
        }
    }

    protected abstract void addAttributeProcessors();

    public boolean handleAttribute(V view, int attributeId, Value value) {
        int position = getPosition(attributeId);
        if (position < 0) {
            //noinspection unchecked
            return null != parent && parent.handleAttribute(view, attributeId, value);
        }
        AttributeProcessor attributeProcessor = processors[position];
        //noinspection unchecked
        attributeProcessor.process(view, value);
        return true;
    }

    public boolean handleChildren(ProteusView view, Value children) {
        return null != parent && parent.handleChildren(view, children);
    }

    public boolean addView(ProteusView parent, ProteusView view) {
        return null != this.parent && this.parent.addView(parent, view);
    }

    @NonNull
    public AttributeSet prepare(@Nullable TypeParser parent) {
        this.parent = parent;
        this.processors = new AttributeProcessor[0];
        this.attributes = new HashMap<>();
        this.offset = null != parent ? parent.getAttributeSet().getOffset() : 0;
        addAttributeProcessors();
        this.attributeSet = new AttributeSet(attributes.size() > 0 ? attributes : null, null != parent ? parent.getAttributeSet() : null);
        return attributeSet;
    }

    public void addAttributeProcessor(String name, AttributeProcessor<V> processor) {
        addAttributeProcessor(processor);
        attributes.put(name, new AttributeSet.Attribute(getAttributeId(processors.length - 1), processor));
    }

    private void addAttributeProcessor(AttributeProcessor<V> handler) {
        processors = Arrays.copyOf(processors, processors.length + 1);
        processors[processors.length - 1] = handler;
    }

    private int getOffset() {
        return offset;
    }

    private int getPosition(int attributeId) {
        return attributeId + getOffset();
    }

    private int getAttributeId(int position) {
        return position - getOffset();
    }

    public int getAttributeId(String name) {
        AttributeSet.Attribute attribute = attributeSet.getAttribute(name);
        return null != attribute ? attribute.id : -1;
    }

    public AttributeSet getAttributeSet() {
        return this.attributeSet;
    }

    private ViewGroup.LayoutParams generateDefaultLayoutParams(ViewGroup parent) {

        /**
         * This whole method is a hack! To generate layout params, since no other way exists.
         * Refer : http://stackoverflow.com/questions/7018267/generating-a-layoutparams-based-on-the-type-of-parent
         */
        if (null == sParser) {
            synchronized (TypeParser.class) {
                if (null == sParser) {
                    initializeAttributeSet(parent);
                }
            }
        }

        return parent.generateLayoutParams(sParser);
    }

    private void initializeAttributeSet(ViewGroup parent) {
        sParser = parent.getResources().getLayout(R.layout.layout_params_hack);
        //noinspection StatementWithEmptyBody
        try {
            //noinspection StatementWithEmptyBody
            while (sParser.nextToken() != XmlPullParser.START_TAG) {
                // Skip everything until the view tag.
            }
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
    }

    public static class AttributeSet {

        @Nullable
        private final Map<String, Attribute> attributes;

        @Nullable
        private final AttributeSet parent;

        private final int offset;

        private AttributeSet(@Nullable Map<String, Attribute> attributes, @Nullable AttributeSet parent) {
            this.attributes = attributes;
            this.parent = parent;
            int parentOffset = null != parent ? parent.getOffset() : 0;
            int length = null != attributes ? attributes.size() : 0;
            this.offset = parentOffset - length;
        }

        @Nullable
        public Attribute getAttribute(String name) {
            Attribute attribute = null != attributes ? attributes.get(name) : null;
            if (null != attribute) {
                return attribute;
            } else if (null != parent) {
                return parent.getAttribute(name);
            } else {
                return null;
            }
        }

        int getOffset() {
            return offset;
        }

        public static class Attribute {

            public final int id;
            public final AttributeProcessor processor;

            public Attribute(int id, AttributeProcessor processor) {
                this.processor = processor;
                this.id = id;
            }
        }
    }

}