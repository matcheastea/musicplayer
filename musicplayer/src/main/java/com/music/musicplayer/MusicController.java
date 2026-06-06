package com.music.musicplayer;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

// PASTIKAN: Nama kelasnya adalah MusicController
public class MusicController {

    @FXML private TableView<Song> songTable;
    @FXML private TableColumn<Song, Integer> colId;
    @FXML private TableColumn<Song, String> colTitle;
    @FXML private TableColumn<Song, String> colArtist;
    @FXML private TableColumn<Song, String> colAlbum;
    @FXML private Label lblNowPlaying;

    private ObservableList<Song> songList = FXCollections.observableArrayList();
    private MediaPlayer mediaPlayer;
    private Song currentSong;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colArtist.setCellValueFactory(new PropertyValueFactory<>("artist"));
        colAlbum.setCellValueFactory(new PropertyValueFactory<>("album"));

        loadSongsFromDatabase();

        songTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                currentSong = newSelection;
                lblNowPlaying.setText("Selected: " + currentSong.getTitle());
            }
        });
    }

    private void loadSongsFromDatabase() {
        songList.clear();
        String query = "SELECT * FROM songs";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                songList.add(new Song(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("artist"),
                        rs.getString("album"),
                        rs.getString("file_path")
                ));
            }
            songTable.setItems(songList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ⚠️ SANGAT PENTING: Harus ada anotasi @FXML dan access modifier public / protected
    @FXML
    protected void onAddSongClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Audio Files", "*.mp3"));
        File selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile != null) {
            String title = selectedFile.getName().replace(".mp3", "");
            String filePath = selectedFile.getAbsolutePath();

            String query = "INSERT INTO songs (title, artist, album, file_path, duration) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {
                 
                pstmt.setString(1, title);
                pstmt.setString(2, "Unknown Artist");
                pstmt.setString(3, "Unknown Album");
                pstmt.setString(4, filePath.replace("\\", "/"));
                pstmt.setInt(5, 0);
                
                pstmt.executeUpdate();

                loadSongsFromDatabase();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    protected void onPlayClick() {
        if (currentSong == null) return;

        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }

        try {
            File file = new File(currentSong.getFilePath());
            if (file.exists()) {
                Media media = new Media(file.toURI().toString());
                mediaPlayer = new MediaPlayer(media);
                mediaPlayer.play();
                lblNowPlaying.setText("Now Playing: " + currentSong.getTitle());
            } else {
                lblNowPlaying.setText("File tidak ditemukan di lokal komputer!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML protected void onPauseClick() { if (mediaPlayer != null) mediaPlayer.pause(); }
    @FXML protected void onStopClick() { if (mediaPlayer != null) mediaPlayer.stop(); }
    @FXML protected void onPrevClick() {}
    @FXML protected void onNextClick() {}
}