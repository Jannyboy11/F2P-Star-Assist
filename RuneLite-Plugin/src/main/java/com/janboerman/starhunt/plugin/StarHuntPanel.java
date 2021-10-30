package com.janboerman.starhunt.plugin;

import com.janboerman.starhunt.common.CrashedStar;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class StarHuntPanel extends PluginPanel {

    private static final JPanel NO_STARS_PANEL = new JPanel(); static {
        JLabel text = new JLabel("There are currently no known stars");
        text.setFont(FontManager.getRunescapeSmallFont());
        NO_STARS_PANEL.add(text);
    }

    private final StarHuntPlugin plugin;
    private final StarHuntConfig config;
    private final List<CrashedStar> starList = new ArrayList<>(2);

    public StarHuntPanel(StarHuntPlugin plugin, StarHuntConfig config) {
        this.plugin = plugin;
        this.config = config;

        setLayout(new GridLayout(0, 1));


    }

    public void setStars(Collection<CrashedStar> starList) {
        this.removeAll();

        this.starList.clear();

        if (starList.isEmpty()) {
            add(NO_STARS_PANEL);
        } else {
            this.starList.addAll(starList);
            this.starList.sort(Comparator.comparing(CrashedStar::getTier).reversed());

            //re-paint
            for (CrashedStar star : this.starList) {
                add(new StarRow(star), BorderLayout.SOUTH);
            }
        }
    }

    private class StarRow extends JPanel {

        private final CrashedStar star;

        private Color lastBackGround;

        StarRow(CrashedStar star) {
            this.star = star;
            setBackground(ColorScheme.MEDIUM_GRAY_COLOR);

            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(2, 0, 2, 0));

            setToolTipText("Click to hop to this world.");

            String text = "T" + star.getTier().getSize() + " W" + star.getWorld() + " " + star.getLocation();   //TODO friendlier-name for the location?
            JLabel textLabel = new JLabel(text);
            textLabel.setFont(FontManager.getRunescapeSmallFont());
            add(textLabel);

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    if (event.getClickCount() == 2) {
                        plugin.hopAndHint(star);
                    }
                }

                @Override
                public void mousePressed(MouseEvent event) {
                    if (event.getClickCount() == 2) {
                        setBackground(getBackground().brighter());
                    }
                }

                @Override
                public void mouseReleased(MouseEvent event) {
                    if (event.getClickCount() == 2) {
                        setBackground(getBackground().darker());
                    }
                }

                @Override
                public void mouseEntered(MouseEvent event) {
                    lastBackGround = getBackground();
                    setBackground(getBackground().brighter());
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    setBackground(lastBackGround);
                }
            });
        }
    }

}

