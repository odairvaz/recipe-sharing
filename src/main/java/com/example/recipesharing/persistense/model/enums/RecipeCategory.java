package com.example.recipesharing.persistense.model.enums;

public enum RecipeCategory {

    APPETIZER("Appetizer"),
    MAIN_DISH("Main Dish"),
    SIDE_DISH("Side Dish"),
    DESSERT("Dessert"),
    BREAKFAST("Breakfast"),
    SOUP("Soup"),
    SALAD("Salad"),
    BEVERAGE("Beverage"),
    SNACK("Snack"),
    VEGETARIAN("Vegetarian"),
    VEGAN("Vegan");

    private final String displayName;

    RecipeCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static RecipeCategory fromDisplayName(String text) {
        for (RecipeCategory b : RecipeCategory.values()) {
            if (b.displayName.equalsIgnoreCase(text)) {
                return b;
            }
        }
        return null;
    }
}
