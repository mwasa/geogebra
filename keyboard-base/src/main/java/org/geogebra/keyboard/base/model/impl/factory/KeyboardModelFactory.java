package org.geogebra.keyboard.base.model.impl.factory;

import org.geogebra.keyboard.base.model.KeyboardModel;

/**
 * This class can create {@link KeyboardModel}s of different types.
 * It is not thread safe.
 */
public class KeyboardModelFactory {

    private MathKeyboardFactory mathKeyboardFactory;
    private GreekKeyboardFactory greekKeyboardFactory;
    private FunctionKeyboardFactory functionKeyboardFactory;
    private LetterKeyboardFactory letterKeyboardFactory;
    private SpecialSymbolsKeyboardFactory specialSymbolsKeyboardFactory;

    public KeyboardModel createMathKeyboard(ButtonFactory buttonFactory) {
        if (mathKeyboardFactory == null) {
            mathKeyboardFactory = new MathKeyboardFactory();
        }
        return mathKeyboardFactory.createMathKeyboard(buttonFactory);
    }

    /**
     * this method will be removed, after the Feature Flags are removed.
     * Two different flags for MOB and GGB: MOB_KEYBOARD_BOX_ICONS
     * use {@link #createMathKeyboard(ButtonFactory)} if Feature Flags are removed.
     */
    public KeyboardModel createMathKeyboard(ButtonFactory buttonFactory, boolean boxIcons) {
        if (mathKeyboardFactory == null) {
            mathKeyboardFactory = new MathKeyboardFactory();
        }
        return mathKeyboardFactory.createMathKeyboard(buttonFactory, boxIcons);
    }

    public KeyboardModel createGreekKeyboard(ButtonFactory buttonFactory) {
        if (greekKeyboardFactory == null) {
            greekKeyboardFactory = new GreekKeyboardFactory();
        }
        return greekKeyboardFactory.createGreekKeyboard(buttonFactory);
    }

    public KeyboardModel createFunctionKeyboard(ButtonFactory buttonFactory) {
        if (functionKeyboardFactory == null) {
            functionKeyboardFactory = new FunctionKeyboardFactory();
        }
        return functionKeyboardFactory.createFunctionKeyboard(buttonFactory);
    }

    public KeyboardModel createLetterKeyboard(ButtonFactory buttonFactory, String topRow, String middleRow, String bottomRow) {
        if (letterKeyboardFactory == null) {
            letterKeyboardFactory = new LetterKeyboardFactory();
        }
        return letterKeyboardFactory.createLetterKeyboard(buttonFactory, topRow, middleRow, bottomRow);
    }

    public KeyboardModel createSpecialSymbolsKeyboard(ButtonFactory buttonFactory) {
        if (specialSymbolsKeyboardFactory == null) {
            specialSymbolsKeyboardFactory = new SpecialSymbolsKeyboardFactory();
        }
        return specialSymbolsKeyboardFactory.createSpecialSymbolsKeyboard(buttonFactory);
    }

    /**
     * this method will be removed, after the Feature Flags are removed.
     * Two different flags for MOB and GGB: MOB_KEYBOARD_BOX_ICONS
     * use {@link #createSpecialSymbolsKeyboard(ButtonFactory)} if Feature Flags are removed.
     */
    public KeyboardModel createSpecialSymbolsKeyboard(ButtonFactory buttonFactory, boolean boxIcons) {
        if (specialSymbolsKeyboardFactory == null) {
            specialSymbolsKeyboardFactory = new SpecialSymbolsKeyboardFactory();
        }
        return specialSymbolsKeyboardFactory.createSpecialSymbolsKeyboard(buttonFactory, boxIcons);
    }
}
