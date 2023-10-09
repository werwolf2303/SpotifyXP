package com.spotifyxp.panels;

import com.spotifyxp.PublicValues;
import com.spotifyxp.configuration.ConfigValues;
import com.spotifyxp.deps.se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.Album;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.Track;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.TrackSimplified;
import com.spotifyxp.dpi.JComponentFactory;
import com.spotifyxp.exception.ExceptionDialog;
import com.spotifyxp.factory.Factory;
import com.spotifyxp.lastfm.LFMValues;
import com.spotifyxp.lastfm.LastFMConverter;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.swingextension.ContextMenu;
import com.spotifyxp.guielements.DefTable;
import com.spotifyxp.swingextension.JImagePanel;
import com.spotifyxp.threading.DefThread;
import com.spotifyxp.utils.ClipboardUtil;
import com.spotifyxp.utils.GraphicalMessage;
import com.spotifyxp.utils.TrackUtils;
import com.spotifyxp.utils.Utils;
import com.spotifyxp.deps.de.umass.lastfm.Artist;
import org.apache.hc.core5.http.ParseException;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class ArtistPanel extends JPanel {
    public JTable artistpopularsonglist;
    public JTable artistalbumalbumtable;
    public JTable lastfmartisttable;
    public JScrollPane artistpopularscrollpane;
    public JScrollPane artistalbumscrollpanel;
    public JScrollPane contentPanel;
    public JScrollPane lastfmartistscrollpanel;
    public JLabel artisttitle;
    public JImagePanel artistbackgroundimage;
    public JImagePanel artistimage;
    public ArrayList<String> artistpopularuricache = new ArrayList<>();
    public ArrayList<String> artistalbumuricache = new ArrayList<>();
    public ArrayList<String> lastfmartisturicache = new ArrayList<>();
    public ContextMenu artistpopularsonglistcontextmenu;
    public ContextMenu artistalbumcontextmenu;
    boolean isFirst = false;
    public ArtistPanel() {
        contentPanel = (JScrollPane) JComponentFactory.createJComponent(new JScrollPane());
        contentPanel.setViewportView(this);
        setLayout(null);
        setPreferredSize(new Dimension(800, 1335));

        JLabel artistpopularlabel = (JLabel) JComponentFactory.createJComponent(new JLabel("Popular"));
        artistpopularlabel.setBounds(5, 291, 137, 27);
        add(artistpopularlabel);

        artistpopularlabel.setForeground(PublicValues.globalFontColor);

        artistpopularscrollpane = (JScrollPane) JComponentFactory.createJComponent(new JScrollPane());
        artistpopularscrollpane.setBounds(5, 320, 760, 277);
        add(artistpopularscrollpane);

        artistpopularsonglist = (JTable) JComponentFactory.createJComponent(new DefTable() {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        artistpopularscrollpane.setViewportView(artistpopularsonglist);

        artistpopularsonglistcontextmenu = new ContextMenu(artistpopularsonglist);
        artistpopularsonglistcontextmenu.addItem(PublicValues.language.translate("ui.general.copyuri"), new Runnable() {
            @Override
            public void run() {
                ClipboardUtil.set(artistpopularuricache.get(artistpopularsonglist.getSelectedRow()));
            }
        });

        artistalbumscrollpanel = (JScrollPane) JComponentFactory.createJComponent(new JScrollPane());
        artistalbumscrollpanel.setBounds(5, 667, 760, 295);
        add(artistalbumscrollpanel);

        artistalbumalbumtable = (JTable) JComponentFactory.createJComponent(new DefTable() {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        artistalbumscrollpanel.setViewportView(artistalbumalbumtable);

        contentPanel.getVerticalScrollBar().setUnitIncrement(20);

        JLabel artistalbumlabel = (JLabel) JComponentFactory.createJComponent(new JLabel("Albums"));
        artistalbumlabel.setBounds(5, 642, 102, 14);
        add(artistalbumlabel);

        artistalbumlabel.setForeground(PublicValues.globalFontColor);

        artistimage = (JImagePanel) JComponentFactory.createJComponent(new JImagePanel());
        artistimage.setBounds(288, 11, 155, 153);
        add(artistimage, 3);

        artisttitle = (JLabel) JComponentFactory.createJComponent(new JLabel(""));
        artisttitle.setHorizontalAlignment(SwingConstants.CENTER);
        artisttitle.setBounds(0, 213, 780, 64);
        add(artisttitle, 2);

        artisttitle.setForeground(PublicValues.globalFontColor);

        artistbackgroundimage = (JImagePanel) JComponentFactory.createJComponent(new JImagePanel());
        artistbackgroundimage.setBounds(0, 0, 780, 277);
        add(artistbackgroundimage, 1);

        artistalbumcontextmenu = new ContextMenu(artistalbumalbumtable);
        artistalbumcontextmenu.addItem(PublicValues.language.translate("ui.general.copyuri"), () -> ClipboardUtil.set(artistalbumuricache.get(artistalbumalbumtable.getSelectedRow())));

        artistalbumalbumtable.setForeground(PublicValues.globalFontColor);
        artistalbumalbumtable.getTableHeader().setForeground(PublicValues.globalFontColor);

        artistalbumalbumtable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getClickCount() == 2) {
                    ContentPanel.isLastArtist = true;
                    ContentPanel.artistPanel.contentPanel.setVisible(false);
                    ContentPanel.searchplaylistpanel.setVisible(true);
                    ContentPanel.searchplaylistsongscache.clear();
                    ((DefaultTableModel) ContentPanel.searchplaylisttable.getModel()).setRowCount(0);
                    try {
                        Album album = Factory.getSpotifyApi().getAlbum(artistalbumuricache.get(artistalbumalbumtable.getSelectedRow()).split(":")[2]).build().execute();
                        for (TrackSimplified simplified : album.getTracks().getItems()) {
                            ((DefaultTableModel) ContentPanel.searchplaylisttable.getModel()).addRow(new Object[]{simplified.getName(), TrackUtils.calculateFileSizeKb(simplified.getDurationMs()), TrackUtils.getBitrate(), TrackUtils.getHHMMSSOfTrack(simplified.getDurationMs())});
                            ContentPanel.searchplaylistsongscache.add(simplified.getUri());
                        }
                    } catch (IOException | ParseException | SpotifyWebApiException ex) {
                        ExceptionDialog.open(ex);
                        ConsoleLogging.Throwable(ex);
                    }
                }
            }
        });

        artistpopularsonglist.setModel(new DefaultTableModel(
                new Object[][]{
                },
                new String[]{
                        PublicValues.language.translate("ui.search.songlist.songname"), PublicValues.language.translate("ui.search.songlist.filesize"), PublicValues.language.translate("ui.search.songlist.bitrate"), PublicValues.language.translate("ui.search.songlist.length")
                }
        ));
        artistpopularsonglist.setForeground(PublicValues.globalFontColor);
        artistpopularsonglist.getTableHeader().setForeground(PublicValues.globalFontColor);

        artistalbumalbumtable.setModel(new DefaultTableModel(
                new Object[][]{
                },
                new String[]{
                        PublicValues.language.translate("ui.search.songlist.songname")
                }
        ));

        artistpopularsonglist.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getClickCount() == 2) {
                    ContentPanel.player.getPlayer().load(artistpopularuricache.get(artistpopularsonglist.getSelectedRow()), true, false, false);
                    TrackUtils.addAllToQueue(artistpopularuricache, artistpopularsonglist);
                }
            }
        });

        lastfmartisttable = (JTable) JComponentFactory.createJComponent(new DefTable() {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });

        lastfmartisttable.setModel(new DefaultTableModel(
                new Object[][]{
                },
                new String[]{
                        PublicValues.language.translate("ui.artist.tablename")
                }
        ));

        lastfmartistscrollpanel = new JScrollPane(lastfmartisttable);

        lastfmartisttable.setForeground(PublicValues.globalFontColor);
        lastfmartisttable.getTableHeader().setForeground(PublicValues.globalFontColor);

        JLabel lastfmsimilarartistslabel = (JLabel) JComponentFactory.createJComponent(new JLabel("Last.fm  Similar Artists"));
        lastfmsimilarartistslabel.setBounds(5, 1000, 212, 18);
        add(lastfmsimilarartistslabel);

        lastfmartisttable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() == 2) {
                    prepareSwitch();
                    try {
                        com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.Artist a = Factory.getSpotifyApi().getArtist(lastfmartisturicache.get(lastfmartisttable.getSelectedRow()).split(":")[2]).build().execute();
                        try {
                            artistimage.setImage(new URL(a.getImages()[0].getUrl()).openStream());
                        } catch (ArrayIndexOutOfBoundsException exception) {
                            //No artist image (when this is raised it's a bug)
                        }
                        artisttitle.setText(a.getName());
                        DefThread trackthread = new DefThread(new Runnable() {
                            public void run() {
                                try {
                                    for (Track t : Factory.getSpotifyApi().getArtistsTopTracks(lastfmartisturicache.get(lastfmartisttable.getSelectedRow()).split(":")[2], ContentPanel.countryCode).build().execute()) {
                                        artistpopularuricache.add(t.getUri());
                                        Factory.getSpotifyAPI().addSongToList(TrackUtils.getArtists(t.getArtists()), t, artistpopularsonglist);
                                    }
                                } catch (IOException | ParseException | SpotifyWebApiException ex) {
                                    ConsoleLogging.Throwable(ex);
                                }
                            }
                        });
                        DefThread albumthread = new DefThread(new Runnable() {
                            public void run() {
                                Factory.getSpotifyAPI().addAllAlbumsToList(artistalbumuricache, lastfmartisturicache.get(lastfmartisttable.getSelectedRow()), artistalbumalbumtable);
                            }
                        });
                        albumthread.start();
                        trackthread.start();
                        lastfmsimilarartistslabel.setEnabled(false);
                        lastfmartisttable.setEnabled(false);
                        PublicValues.blockArtistPanelBackButton = true;
                        javax.swing.SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                contentPanel.getVerticalScrollBar().setValue(0);
                            }
                        });
                        ContentPanel.artistPanelBackButton.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                if(!PublicValues.blockArtistPanelBackButton) {
                                    return;
                                }
                                lastfmsimilarartistslabel.setEnabled(true);
                                lastfmartisttable.setEnabled(false);
                                restoreCache();
                                clearCache();
                                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        contentPanel.getVerticalScrollBar().setValue(0);
                                    }
                                });
                                DefThread thread = new DefThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        while(ContentPanel.artistPanelBackButton.getModel().isPressed()) {
                                            try {
                                                Thread.sleep(TimeUnit.SECONDS.toMillis(1));
                                            }catch (Exception ignored) {
                                            }
                                        }
                                        PublicValues.blockArtistPanelBackButton = false;
                                    }
                                });
                                thread.start();
                            }
                        });
                        finalizeSwitch();
                    }catch (Exception e2) {
                        throw new RuntimeException(e2);
                    }
                }
            }
        });

        lastfmsimilarartistslabel.setForeground(PublicValues.globalFontColor);

        lastfmartistscrollpanel.setBounds(5, 1035, 760, 295);
        add(lastfmartistscrollpanel);
    }

    ArrayList<String> albumuricache = new ArrayList<>();
    ArrayList<String> popularuricache = new ArrayList<>();
    ArrayList<String> lastfmuricache = new ArrayList<>();
    ArrayList<ArrayList<Object>> albumcache = new ArrayList<>();
    ArrayList<ArrayList<Object>> popularcache = new ArrayList<>();
    ArrayList<ArrayList<Object>> lastfmcache = new ArrayList<>();
    String artistcache;
    InputStream artistbackgroundcache;
    InputStream artistimagecache;

    public void buildCache() {
        albumuricache = artistalbumuricache;
        popularuricache = artistpopularuricache;
        lastfmuricache = lastfmartisturicache;
        albumcache = parseTable(artistalbumalbumtable);
        popularcache = parseTable(artistpopularsonglist);
        lastfmcache = parseTable(lastfmartisttable);
        artistcache = artisttitle.getText();
        artistimagecache = artistimage.getImageStream();
        if(artistbackgroundimage.getImageStream() != null) artistbackgroundcache = artistbackgroundimage.getImageStream();
    }

    public void finalizeSwitch() {
        ((DefaultTableModel) lastfmartisttable.getModel()).setRowCount(0);
        lastfmartisturicache.clear();
        contentPanel.getHorizontalScrollBar().setValue(contentPanel.getHorizontalScrollBar().getMinimum());
    }

    ArrayList<ArrayList<Object>> parseTable(JTable table) {
        ArrayList<ArrayList<Object>> objects = new ArrayList<>();
        for(int row = 0; row < table.getModel().getRowCount(); row++) {
            ArrayList<Object> rows = new ArrayList<>();
            for (int col = 0; col < table.getModel().getColumnCount(); col++) {
                rows.add(table.getModel().getValueAt(row, col));
            }
            objects.add(rows);
        }
        return objects;
    }

    public void restoreTable(JTable table, ArrayList<ArrayList<Object>> rows) {
        ((DefaultTableModel) table.getModel()).setRowCount(0);
        for(ArrayList<Object> o : rows) {
            ((DefaultTableModel) table.getModel()).addRow(o.toArray());
        }
    }

    public void prepareSwitch() {
        buildCache();
        ((DefaultTableModel) artistalbumalbumtable.getModel()).setRowCount(0);
        artistalbumuricache.clear();
        artisttitle.setText("");
        ((DefaultTableModel) artistpopularsonglist.getModel()).setRowCount(0);
        artistpopularuricache.clear();
    }

    public void restoreCache() {
        restoreTable(lastfmartisttable, lastfmcache);
        restoreTable(artistalbumalbumtable, albumcache);
        restoreTable(artistpopularsonglist, popularcache);
        artistalbumuricache = albumuricache;
        artistpopularuricache = popularuricache;
        lastfmartisturicache = lastfmuricache;
        artisttitle.setText(artistcache);
        artistimage.setImage(artistimagecache);
        if(artistbackgroundcache != null) artistbackgroundimage.setImage(artistbackgroundcache);
    }

    public void clearCache() {
        albumuricache = new ArrayList<>();
        popularuricache = new ArrayList<>();
        lastfmuricache = new ArrayList<>();
        albumcache = new ArrayList<>();
        popularcache = new ArrayList<>();
        lastfmcache = new ArrayList<>();
        artistcache = "";
        artistbackgroundcache = null;
        artistimagecache = null;
    }

    public void openPanel() {
        lastfmartisturicache.clear();
        ((DefaultTableModel) lastfmartisttable.getModel()).setRowCount(0);
        DefThread thread = new DefThread(new Runnable() {
            @Override
            public void run() {
                if(PublicValues.config.get(ConfigValues.lastfmusername.name).equalsIgnoreCase("")) {
                    return;
                }
                for(Artist a : Artist.getSimilar(artisttitle.getText(), 10, LFMValues.apikey)) {
                    ((DefaultTableModel) lastfmartisttable.getModel()).addRow(new Object[]{a.getName()});
                    lastfmartisturicache.add(LastFMConverter.getArtistURIfromName(a.getName()));
                }
            }
        });
        thread.start();
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                contentPanel.getVerticalScrollBar().setValue(0);
            }
        });
    }
}
