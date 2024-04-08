import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import models.Baseline;
import org.jgrapht.alg.util.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class GroundTruth extends Baseline {
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

  private List<Pair<String, String>> readInRangeGroundTruth() throws IOException, CsvException {
    Reader reader = Files.newBufferedReader(Path.of(gt_path));
    CSVReader csvReader = new CSVReader(reader);
    // List<String[]> records = csvReader.readAll();
    // 错误: 未报告的异常错误CsvException; 必须对其进行捕获或声明以便抛出
    // csvReader.readAll();
    List<String[]> records = csvReader.readAll();
    List<Pair<String, String>> res = new ArrayList<>();
    for (String[] record : records) {
      res.add(new Pair<>(record[2], record[5]));
    }
    return res;
  }

  private List<Pair<String, String>> readInOurResults(String resPath) throws IOException {
    List<Pair<String, String>> res = new ArrayList<>();
    BufferedReader reader = Files.newBufferedReader(Path.of(resPath));
    for (String line = reader.readLine(); line != null; line = reader.readLine()) {
      String[] parts = line.split(",");
      if (parts.length != 2) {
        throw new RuntimeException("Invalid format in " + resPath);
      }
      res.add(
          new Pair<>(
              parts[0].substring(1, parts[0].length() - 1),
              parts[1].substring(1, parts[1].length() - 1)));
    }
    return res;
  }

  public void validate(String resPath) {
    try {
      List<Pair<String, String>> gt = readInRangeGroundTruth();
      List<Pair<String, String>> ours = readInOurResults(resPath);
      int correct = 0;
      for (Pair<String, String> pair : ours) {
        if (gt.contains(pair)) {
          correct++;
        }
      }
      System.out.println("Acc: " + (double) correct / ours.size() + "\n");
      System.out.println("Recall: " + (double) correct / gt.size() + "\n");
    } catch (IOException | CsvException e) {
      e.printStackTrace();
    }
  }
}
