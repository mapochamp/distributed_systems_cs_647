import org.apache.spark.sql.{SparkSession, Dataset, Row}
import org.apache.spark.sql.functions._

case class Email(file: String, text: String, label: Double)

object NaiveBayesExample {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder.appName("NaiveBayesExample").getOrCreate()
    import spark.implicits._

    // Load training data
    val trainSpamDirs = (1 to 2).map(i => s"data/train/enron$i/spam")
    val trainHamDirs = (1 to 2).map(i => s"data/train/enron$i/ham")
    
    val trainData = trainSpamDirs.map(dir => loadData(spark, dir, 1.0)).reduce(_ union _)
      .union(trainHamDirs.map(dir => loadData(spark, dir, 0.0)).reduce(_ union _))

    // Load test data
    val testSpamDir = "data/test/enron6/spam"
    val testHamDir = "data/test/enron6/ham"

    val testData = loadData(spark, testSpamDir, 1.0).union(loadData(spark, testHamDir, 0.0))

    // Tokenize the text
    val trainTokens = trainData.map(email => (email.label, email.text.split(" ")))
    val testTokens = testData.map(email => (email.label, email.text.split(" ")))

    println(s"count # of items")
    println(s"$testData.count()")
    println(s"First item")
    println(testData.first())
  }

  def loadData(spark: SparkSession, directory: String, label: Double): Dataset[Email] = {
    import spark.implicits._

    val data = spark.sparkContext.wholeTextFiles(directory)
      .map { case (file, text) => Email(file, text, label) }
      .toDS()

    data
  }
}
