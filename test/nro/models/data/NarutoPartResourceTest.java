package nro.models.data;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;

public final class NarutoPartResourceTest {

    private static final int[] SMALL_ICON_IDS = {
        25131, 25210, 25211, 25238, 25239,
        25275, 25348, 25349, 25351
    };

    private NarutoPartResourceTest() {
    }

    public static void main(String[] args) throws Exception {
        List<Integer> frameIds = frameIds();
        require(frameIds.size() == 122, "Expected 122 distinct Naruto frame IDs");
        require(new HashSet<>(frameIds).size() == frameIds.size(), "Duplicate Naruto frame ID");

        int verifiedFrames = 0;
        for (int zoom = 1; zoom <= 4; zoom++) {
            for (int id : frameIds) {
                Path res = Path.of("data", "res", "x" + zoom, id + ".png");
                Path icon = Path.of("data", "icon", "x" + zoom, id + ".png");
                requirePng(res);
                requirePng(icon);
                require(Arrays.equals(Files.readAllBytes(res), Files.readAllBytes(icon)),
                        "Frame differs between data/res and data/icon: x" + zoom + "/" + id);
                verifiedFrames++;
            }
            for (int id : SMALL_ICON_IDS) {
                requirePng(Path.of("data", "icon", "x" + zoom, id + ".png"));
            }
        }

        System.out.println("NARUTO_PART_RESOURCE_OK frames=" + verifiedFrames
                + " smallIcons=" + (SMALL_ICON_IDS.length * 4));
    }

    private static List<Integer> frameIds() {
        List<Integer> ids = new ArrayList<>();
        addRange(ids, 25124, 25130);
        addRange(ids, 25183, 25209);
        addRange(ids, 25212, 25237);
        ids.add(25240);
        addRange(ids, 25241, 25274);
        addRange(ids, 25322, 25347);
        ids.add(25350);
        return ids;
    }

    private static void addRange(List<Integer> ids, int first, int last) {
        for (int id = first; id <= last; id++) {
            ids.add(id);
        }
    }

    private static void requirePng(Path path) throws IOException {
        require(Files.isRegularFile(path), "Missing PNG: " + path);
        BufferedImage image = ImageIO.read(path.toFile());
        require(image != null && image.getWidth() > 0 && image.getHeight() > 0,
                "Invalid PNG: " + path);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
