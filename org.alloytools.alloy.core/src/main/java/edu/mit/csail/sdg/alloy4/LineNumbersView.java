package edu.mit.csail.sdg.alloy4;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.Utilities;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.font.FontRenderContext;
import java.util.Collections;
import java.util.Objects;
/*
 * Modified 2022-01 from code from https://github.com/hblok/rememberjava/blob/master/_includes/src/com/rememberjava/ui/RowHeaderViewLineNumbers.java itself
 * under the GPL 3.
 */

/**
 * Provides display of line-numbers to a provided JTextComponent.
 * An instance is used to populate the rowHeader property of a JScrollPane instance,
 * based on information and events from the JTextComponent instance supplied
 * at initialization.
 * <p>
 * Uses fontName and fontSize provided at initialization time, and as updated
 * by callers.  Follows the anti-aliasing policy that {@link OurAntiAlias} follows, which
 * is not tied to user preferences.
 * <p>
 * Makes a margin at least large enough for 3 digits, and can grow or shrink as needed.
 * The current font is used to determine how wide the needed digits require.
 */

public class LineNumbersView extends JComponent implements DocumentListener, CaretListener, ComponentListener {

    private static final long serialVersionUID = 1L;

    private final JTextComponent editor;
    private final boolean antiAlias;

    private int marginWidth;
    private boolean lineNumbers;
    private String fontName;
    private int fontSize;
    private Font font;

    public LineNumbersView(JTextComponent editor, boolean shouldDisplay, String fontName, int fontSize) {
        Objects.requireNonNull(editor, "Need a non-null JTextComponent for parameter editor");
        this.editor = editor;
        this.antiAlias = Util.onMac() || Util.onWindows();

        if (fontName != null && fontSize > 1) {
            this.fontName = fontName;
            this.fontSize = fontSize;
        } else {
            this.fontName = Font.MONOSPACED;
            this.fontSize = 14;
        }
        font = new Font(fontName, Font.PLAIN, fontSize);

        this.lineNumbers = shouldDisplay;
        if (lineNumbers) {
            marginWidth = calculateMarginWidth(3);
            editor.getDocument().addDocumentListener(this);
            editor.addComponentListener(this);
            editor.addCaretListener(this);
        } else {
            marginWidth = 0;
        }
    }

    public boolean isAntiAlias() {
        return antiAlias;
    }

    public int getMarginWidth() {
        return marginWidth;
    }

    public boolean isLineNumbers() {
        return lineNumbers;
    }

    public String getFontName() {
        return fontName;
    }

    public int getFontSize() {
        return fontSize;
    }

    @Override
    public Font getFont() {
        return font;
    }

    @Override
    public void paintComponent(Graphics g) {
        if (antiAlias && g instanceof Graphics2D) {
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }
        super.paintComponent(g);

        if (lineNumbers) {
            font = getUpdatedFont();
            Rectangle clip = g.getClipBounds();
            int startOffset = editor.viewToModel(new Point(0, clip.y));
            int endOffset = editor.viewToModel(new Point(0, clip.y + clip.height));

            // track the widest number we've seen ths paintComponent
            int maxDigits = 0;
            // also track the margin width at the start of this paintComponent
            int startMarginWidth = getMarginWidth();

            while (startOffset <= endOffset) {
                try {
                    int lineNumber = getLineNumber(startOffset);
                    if (lineNumber > 0) {
                        int x = getInsets().left + 2;
                        int y = getOffsetY(startOffset);

                        g.setFont(font);
                        Color color = Color.BLACK;
                        if (isCurrentLine(startOffset)) {
                            color = Color.red;
                        }
                        g.setColor(color);
                        final String formattedLineNumber = formatLineNumber(lineNumber);
                        if (formattedLineNumber.length() > maxDigits) {
                            maxDigits = formattedLineNumber.length();
                        }
                        g.drawString(formattedLineNumber, x, y);
                    }
                    startOffset = Utilities.getRowEnd(editor, startOffset) + 1;
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            }

            // if the margin width changed after updateMarginWidth()
            // then we update our size and request repainting.
            updateMarginWidth(maxDigits);
            if (startMarginWidth != getMarginWidth()) {
                updateSizeAndDocumentChanged();
            }
        }
    }

    private void updateSizeAndDocumentChanged() {
        updateSize();
        documentChanged();
    }

    private void updateMarginWidth(int maxDigits) {
        marginWidth = calculateMarginWidth(Math.max(maxDigits, 3));
    }

    /**
     * update the font we will use to draw numbers from the provided editor component.
     */
    private Font getUpdatedFont() {
        if (fontName.equals(font.getFontName()) || fontSize != font.getSize()) {
            return new Font(fontName, Font.PLAIN, fontSize);
        } else {
            return font;
        }
    }

    private int getLineIndex(int offset) {
        Element root = editor.getDocument().getDefaultRootElement();
        return root.getElementIndex(offset);
    }

    private int getLineNumber(int offset) {
        int index = getLineIndex(offset);
        Element root = editor.getDocument().getDefaultRootElement();
        Element line = root.getElement(index);

        return line.getStartOffset() == offset ? index + 1 : 0;
    }

    private String formatLineNumber(int lineNumber) {
        return String.format("%3d", lineNumber);
    }

    private int getOffsetY(int offset) throws BadLocationException {
        FontMetrics fontMetrics = editor.getFontMetrics(editor.getFont());
        int descent = fontMetrics.getDescent();

        Rectangle r = editor.modelToView(offset);
        return r.y + r.height - descent;
    }

    private boolean isCurrentLine(int offset) {
        int caretPosition = editor.getCaretPosition();
        Element root = editor.getDocument().getDefaultRootElement();
        return root.getElementIndex(offset) == root.getElementIndex(caretPosition);
    }

    private void documentChanged() {
        SwingUtilities.invokeLater(this::repaint);
    }

    private void updateSize() {
        Dimension size = new Dimension(marginWidth, editor.getHeight());
        setPreferredSize(size);
        setSize(size);
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        documentChanged();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        documentChanged();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        documentChanged();
    }

    @Override
    public void caretUpdate(CaretEvent e) {
        updateSizeAndDocumentChanged();
    }

    @Override
    public void componentResized(ComponentEvent e) {
        updateSizeAndDocumentChanged();
    }

    @Override
    public void componentMoved(ComponentEvent e) {
        updateSizeAndDocumentChanged();
    }

    @Override
    public void componentShown(ComponentEvent e) {
        updateSizeAndDocumentChanged();
    }

    @Override
    public void componentHidden(ComponentEvent e) {
    }

    public void updateFontNameAndFontSize(String fontName, int fontSize) {
        this.fontName = fontName;
        this.fontSize = fontSize;
        font = getUpdatedFont();
        updateSizeAndDocumentChanged();
    }

    public void enableLineNumbers(boolean flag) {
        lineNumbers = flag;
        if (lineNumbers) {
            marginWidth = calculateMarginWidth(3);
            editor.getDocument().addDocumentListener(this);
            editor.addComponentListener(this);
            editor.addCaretListener(this);
        } else {
            marginWidth = 0;
        }
        font = getUpdatedFont();
        updateSizeAndDocumentChanged();
    }

    /**
     * produce a margin width based on a number of digits to display.
     */
    private int calculateMarginWidth(int digits) {
        if (font != null) {
            String toFit = String.join("", Collections.nCopies(digits, "0"));
            return (int) Math.ceil(font
                    .getStringBounds(toFit, new FontRenderContext(null, antiAlias, false))
                    .getWidth() * 1.2);
        } else {
            return fontSize * digits;
        }

    }


}