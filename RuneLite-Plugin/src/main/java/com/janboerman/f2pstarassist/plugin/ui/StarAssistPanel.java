package com.janboerman.f2pstarassist.plugin.ui;

import com.google.common.html.HtmlEscapers;
import com.janboerman.f2pstarassist.plugin.StarAssistConfig;
import com.janboerman.f2pstarassist.plugin.model.CrashedStar;
import com.janboerman.f2pstarassist.plugin.model.DiscordUser;
import com.janboerman.f2pstarassist.plugin.model.RunescapeUser;
import com.janboerman.f2pstarassist.plugin.model.User;
import com.janboerman.f2pstarassist.plugin.StarAssistPlugin;
import com.janboerman.f2pstarassist.plugin.StarPoints;

import net.runelite.api.FriendsChatRank;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;

public class StarAssistPanel extends PluginPanel {

    private static final JPanel NO_STARS_PANEL = new JPanel(); static {
        JLabel text = new JLabel("There are currently no known stars.");
        text.setFont(FontManager.getRunescapeSmallFont());
        NO_STARS_PANEL.add(text);
    }

    private final StarAssistPlugin plugin;
    private final ClientThread clientThread;
    private final List<CrashedStar> starList = new ArrayList<>(2);

    private FriendsChatRank f2pStarHuntRank;

    public StarAssistPanel(StarAssistPlugin plugin, ClientThread clientThread) {
        this.plugin = plugin;
        this.clientThread = clientThread;

        setLayout(new GridLayout(0, 1));
    }

    private static Comparator<CrashedStar> compareByLocation(@Nullable WorldPoint playerLocation) {
        if (playerLocation != null && playerLocation.getPlane() == 0) {
            return (star1, star2) -> {
                WorldPoint star1Point = StarPoints.fromLocation(star1.getLocation());
                WorldPoint star2Point = StarPoints.fromLocation(star2.getLocation());
                return Integer.compare(star1Point.distanceTo2D(playerLocation), star2Point.distanceTo2D(playerLocation));
            };
        } else {
            return Comparator.comparing(CrashedStar::getLocation);
        }
    }

    public void setStars(Collection<CrashedStar> starList, @Nullable WorldPoint playerLocation, FriendsChatRank f2pStarHuntRank, boolean httpEnabled) {
        this.removeAll();

        this.starList.clear();
        this.f2pStarHuntRank = f2pStarHuntRank;

        if (starList.isEmpty()) {
            add(NO_STARS_PANEL, BorderLayout.SOUTH); // TODO check: is BorderLayout.SOUTH what we want?
        } else {
            this.starList.addAll(starList);
            this.starList.sort(compareByLocation(playerLocation).thenComparing(Comparator.comparing(CrashedStar::getTier).reversed()));

            //re-paint
            for (CrashedStar star : this.starList) {
                //TODO don't add to the root panel, use a separate panel instead, that itself is added to the root panel.
                add(new StarAssistPanelRow(star), BorderLayout.SOUTH);
            }
        }

        // TODO: find out why this button does not seem to render.
        if (StarAssistPlugin.isRanked(f2pStarHuntRank) && httpEnabled) {
            add(Box.createVerticalStrut(2), BorderLayout.SOUTH);

            JButton refreshButton = new JButton("Refresh");
            refreshButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            refreshButton.setFont(FontManager.getRunescapeFont());
            refreshButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    plugin.fetchStarList();
                }
            });
            refreshButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    setBackground(ColorScheme.DARKER_GRAY_COLOR);
                }
            });
            add(refreshButton, BorderLayout.SOUTH);
        }

        revalidate();
        repaint();
    }

    private class StarAssistPanelRow extends JPanel {

        private final JMenuItem removeMenuItem;
        private final JMenuItem copyToClipboardMenuItem;
        private final JMenuItem publishStarMenuItem;

        private Color lastBackGround;

        StarAssistPanelRow(CrashedStar star) {
            setBackground(ColorScheme.MEDIUM_GRAY_COLOR);

            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(2, 2, 2, 2));

            String foundBy = null;
            final User finder = star.getDiscoveredBy();
            if (finder instanceof RunescapeUser) {
                foundBy = ((RunescapeUser) finder).getName();
            } else if (finder instanceof DiscordUser) {
                foundBy = ((DiscordUser) finder).getName();
            }

            if (foundBy != null) {
                foundBy = HtmlEscapers.htmlEscaper().escape(foundBy);
            }

            final Instant detectedAt = star.getDetectedAt();
            final LocalDateTime localDateTime = LocalDateTime.ofInstant(detectedAt, ZoneId.systemDefault());
            final String foundAt = localDateTime.format(DateTimeFormatter.ofPattern("HH:mm"));

            final StringJoiner tooltipText = new StringJoiner("<br>", "<html>", "</html>");
            if (foundBy != null)
                tooltipText.add("Found by: " + foundBy);
            tooltipText.add("Found at: " + foundAt);
            tooltipText.add("Double click to hop to this world.");

            setToolTipText(tooltipText.toString());

            final String text = "T" + star.getTier().getSize() + " W" + star.getWorld() + " " + star.getLocation();
            final JLabel textLabel = new JLabel(text);
            textLabel.setFont(FontManager.getRunescapeSmallFont());
            add(textLabel);

            //right-click menu:
            final JPopupMenu popupMenu = new JPopupMenu();
            popupMenu.setBorder(new EmptyBorder(2, 2, 2, 2));
            //right-click -> copy to clipboard option
            copyToClipboardMenuItem = new JMenuItem("Copy to clipboard"); //TODO icon?
            copyToClipboardMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    StringSelection selection = new StringSelection(text);
                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    clipboard.setContents(selection, selection);
                }
            });
            popupMenu.add(copyToClipboardMenuItem);
            //right click -> remove option
            removeMenuItem = new JMenuItem("Remove"); //TODO icon?
            removeMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    starList.remove(star);
                    clientThread.invoke(() -> {
                        //remove from local cache
                        plugin.removeStar(star.getKey());

                        //re-paint panel
                        WorldPoint playerLocation = plugin.getLocalPlayerLocation();
                        FriendsChatRank f2pStarHuntRank = plugin.f2pStarHuntRank();
                        boolean httpEnabled = plugin.httpEnabled();
                        SwingUtilities.invokeLater(() -> setStars(new ArrayList<>(starList), playerLocation, f2pStarHuntRank, httpEnabled));
                    });
                }
            });
            popupMenu.add(removeMenuItem);
            if (star.hasId() && StarAssistPlugin.isRanked(f2pStarHuntRank)) {
                publishStarMenuItem = new JMenuItem("Publish"); // TODO icon?
                publishStarMenuItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        plugin.publishStar(star);
                    }
                });
            } else {
                publishStarMenuItem = null;
            }

            setComponentPopupMenu(popupMenu);

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    //TODO this does not always seem to work reliably:
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

