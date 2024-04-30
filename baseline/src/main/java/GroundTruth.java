import com.opencsv.CSVReader;
import models.Baseline;
import org.jgrapht.alg.util.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GroundTruth extends Baseline {
  private class Location {
    public String file;
    public int start;
    public int end;

    public Location(String file, int start, int end) {
      this.file = file.replaceAll("//", "/");
      if (this.file.startsWith("/")) {
        this.file = this.file.substring(1);
      }
      this.file = file;
      this.start = start;
      this.end = end;
    }

    public Location(String file, String range) {
      this.file = file.replaceAll("//", "/");
      if (this.file.startsWith("/")) {
        this.file = this.file.substring(1);
      }
      String[] parts = range.split("-");
      try {
        this.start = Integer.parseInt(parts[0]);
        this.end = Integer.parseInt(parts[1]);
      } catch (Exception e) {
        System.out.println("Error: " + file + " " + range);
        this.start = -1;
        this.end = -1;
      }
    }

    public boolean equals(Object obj) {
      if (framework.equals("web")) {
        return file.equals(((Location) obj).file)
            && start == ((Location) obj).start
            && end == ((Location) obj).end;
      }
      if (obj instanceof Location) {
        Location loc = (Location) obj;
        return file.equals(loc.file)
            && (start <= loc.start && end >= loc.end || loc.start <= start && loc.end >= end);
      }
      return false;
    }

    @Override
    public String toString() {
      return file + " " + start + "-" + end;
    }
  }

  public String gt_path =
      System.getProperty("user.home")
          + "/coding/xll-gt/"
          + framework
          + "/processed/"
          + projectName
          + ".csv";

  public GroundTruth(String framework, String projectName, String projectDir, String outputDir) {
    super(framework, projectName, projectDir, outputDir);
    gt_path =
        System.getProperty("user.home")
            + "/coding/xll-gt/"
            + framework
            + "/processed/"
            + projectName
            + ".csv";
  }

  private List<Pair<Location, Location>> readInRangeGroundTruth() throws IOException {
    Reader reader = Files.newBufferedReader(Path.of(gt_path));
    CSVReader csvReader = new CSVReader(reader);
    csvReader.readNext();
    List<String[]> records = csvReader.readAll();
    List<Pair<Location, Location>> res = new ArrayList<>();
    for (String[] record : records) {
      res.add(new Pair<>(new Location(record[1], record[2]), new Location(record[4], record[5])));
    }
    return res;
  }

  private List<Pair<Location, Location>> readInOurResults(String resPath) throws IOException {
    List<Pair<Location, Location>> res = new ArrayList<>();
    BufferedReader reader = Files.newBufferedReader(Path.of(resPath));
    CSVReader csvReader = new CSVReader(reader);
    List<String[]> records = csvReader.readAll();
    for (String[] record : records) {
      res.add(new Pair<>(new Location(record[0], record[1]), new Location(record[2], record[3])));
    }
    return res;
  }

  public void validate(String resPath) {
    try {
      List<Pair<Location, Location>> gt = readInRangeGroundTruth();
      List<Pair<Location, Location>> ours = readInOurResults(resPath);

      List<Pair<Location, Location>> fp = new ArrayList<>();
      List<Pair<Location, Location>> fn = new ArrayList<>();
      int correct = 0;
      for (Pair<Location, Location> pair : ours) {
        if (gt.contains(pair)) {
          correct++;
        } else {
          fp.add(pair);
        }
      }
      for (Pair<Location, Location> pair : gt) {
        if (!ours.contains(pair)) {
          fn.add(pair);
        }
      }
      System.out.println("TP: " + correct + "\n");
      System.out.println("FP: " + fp.size() + "\n");
      System.out.println("FN: " + fn.size() + "\n");
      System.out.println("Acc: " + (double) correct / ours.size() + "\n");
      System.out.println("Recall: " + (double) correct / gt.size() + "\n");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
