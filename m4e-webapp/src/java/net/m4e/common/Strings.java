/*
 * Copyright (c) 2017 by Botorabi. All rights reserved.
 * https://github.com/botorabi/Meet4Eat
 * 
 * License: MIT License (MIT), read the LICENSE text in
 *          main directory for more details.
 */

package net.m4e.common;

/**
 * A collection of string related utilities
 * 
 * @author boto
 * Date of creation Sep 5, 2017
 */
public class Strings {

    /**
     * Given an input string, limit its length. The method also handles null as input.
     * 
     * @param input     Input string
     * @param maxLen    Maximal character length
     * @return          Return length limited string, or null if the input was null.
     */
    public static String limitStringLen(String input, int maxLen) {
        if (input != null) {
            if (input.length() > maxLen) {
                input = input.substring(0, maxLen);
            }
        }
        return input;
    }

    /**
     * Check if the input string length is in given min/max range.
     * 
     * @param input    Input string
     * @param minLen   Min length
     * @param maxLen   Max length 
     * @return         Return true if the string has a length in given range, otherwise return false.
     */
    public static boolean checkMinMaxLength(String input, int minLen, int maxLen) {
        if (input == null) {
            return false;
        }
        int len = input.length();
        return (len >= minLen) && (len <= maxLen);
    }
}
