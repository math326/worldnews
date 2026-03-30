package com.worldnews.app.model;

import com.google.gson.annotations.SerializedName;

public class TranslationResponse {

    @SerializedName("responseData")
    private ResponseData responseData;

    public ResponseData getResponseData() {
        return responseData;
    }

    public static class ResponseData {
        @SerializedName("translatedText")
        private String translatedText;

        public String getTranslatedText() {
            return translatedText;
        }
    }
}
