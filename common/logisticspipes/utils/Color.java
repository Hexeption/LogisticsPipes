/*
 * Copyright (c) 2015  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/mc16/LICENSE.md
 */

package logisticspipes.utils;

/**
 * Enumeration for colors and their int values. Also contains some static functions.
 */
public enum Color {
	WHITE(0xFFFFFFFF),
	BLACK(0xFF000000),
	LIGHT_GREY(0xFFC6C6C6),
	MIDDLE_GREY(0xFF8B8B8B),
	DARK_GREY(0xFF555555),
	RED(0xFFFF0000),
	GREEN(0xFF00FF00),
	BLUE(0xFF0000FF);

	private int colorValue;

	Color(int value) {
		colorValue = value;
	}

	public int getValue() {
		return colorValue;
	}

	public static int getValue(Color color) {
		return color.colorValue;
	}

	public static float getAlpha(int colorValue) {
		return (float) (colorValue >> 24 & 255) / 255.0F;
	}

	public static float getRed(int colorValue) {
		return (float) (colorValue >> 16 & 255) / 255.0F;
	}

	public static float getGreen(int colorValue) {
		return (float) (colorValue >> 8 & 255) / 255.0F;
	}

	public static float getBlue(int colorValue) {
		return (float) (colorValue & 255) / 255.0F;
	}
}