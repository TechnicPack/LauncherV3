package net.technicpack.ui.controls;

import net.technicpack.ui.lang.ResourceLoader;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.plaf.metal.MetalToolTipUI;
import java.awt.*;

public class TooltipWarning extends JLabel {

    private JToolTip toolTip;

    public TooltipWarning(Icon icon, JToolTip toolTip) {
        super(icon);
        this.toolTip = toolTip;
    }

    @Override
    public JToolTip createToolTip() {
        WarningTooltip tooltip = new WarningTooltip();
        tooltip.setBackground(this.toolTip.getBackground());
        tooltip.setForeground(this.toolTip.getForeground());
        tooltip.setTipText(this.toolTip.getTipText());
        tooltip.setFont(this.toolTip.getFont());
        tooltip.setBorder(this.toolTip.getBorder());
        return tooltip;
    }

    private class WarningTooltip extends JToolTip {

        public WarningTooltip() {
            setUI(new WarningTooltipUI());
        }
    }

    private class WarningTooltipUI extends MetalToolTipUI {

        @Override
        public void paint(Graphics g, JComponent c) {
            Graphics2D g2d = (Graphics2D)g;
            JToolTip tooltip = ((JToolTip)c);

            g2d.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2d.setRenderingHint(
                    RenderingHints.KEY_FRACTIONALMETRICS,
                    RenderingHints.VALUE_FRACTIONALMETRICS_ON);

            g2d.setColor(tooltip.getBackground());
            g2d.setFont(tooltip.getFont());
            g2d.setColor(tooltip.getForeground());

            if (((JToolTip)c).getTipText() != null)
                drawTextUgly(((JToolTip)c).getTipText(), g2d);
        }

        @Override
        public Dimension getPreferredSize(JComponent c) {
            // Ugly code to wrap text
            JToolTip tooltip = (JToolTip)c;
            String textToDraw = tooltip.getTipText();

            if (textToDraw == null)
                return getMinimumSize(c);

            String[] arr = textToDraw.split(" ");
            int nIndex = 0;
            int startX = 4;
            int startY = 3;
            int lineSize = (int)tooltip.getFontMetrics(tooltip.getFont()).getHeight();

            while ( nIndex < arr.length )
            {
                int nextStartY = startY + lineSize;

                int nextEndY = nextStartY + lineSize;

                String line = arr[nIndex++];
                int lineWidth = tooltip.getFontMetrics(tooltip.getFont()).stringWidth(line);

                while ( ( nIndex < arr.length ) && (lineWidth < 243) )
                {
                    line = line + " " + arr[nIndex];
                    nIndex++;

                    if (nIndex == arr.length)
                        break;

                    lineWidth = tooltip.getFontMetrics(tooltip.getFont()).stringWidth(line+" "+arr[nIndex]);
                }
                startY = nextStartY;
            }

            return new Dimension(248, startY+4);
        }

        private void drawTextUgly(String text, Graphics2D g2)
        {
            // Ugly code to wrap text
            String textToDraw = text;
            String[] arr = textToDraw.split(" ");
            int nIndex = 0;
            int startX = 4;
            int startY = 3;
            int lineSize = (int)g2.getFontMetrics().getHeight();

            while ( nIndex < arr.length )
            {
                int nextStartY = startY + lineSize;

                int nextEndY = nextStartY + lineSize;

                String line = arr[nIndex++];
                int lineWidth = g2.getFontMetrics().stringWidth(line);

                while ( ( nIndex < arr.length ) && (lineWidth < 243) )
                {
                    line = line + " " + arr[nIndex];
                    nIndex++;

                    if (nIndex == arr.length)
                        break;

                    lineWidth = g2.getFontMetrics().stringWidth(line+" "+arr[nIndex]);
                }

                g2.drawString(line, startX, startY + g2.getFontMetrics().getAscent());
                startY = nextStartY;
            }
        }
    }
}
