import org.apache.spark.sql.SparkSession

object naiveBayes {
  def main(args: Array[String]) {
    val ham = "/Users/tajungjang/Downloads/enron1/ham" // Should be some file on your system
    val spam = "/Users/tajungjang/Downloads/enron1/spam" // Should be some file on your system
    val spark = SparkSession.builder.appName("Simple Application").getOrCreate()
    /*
    val logData = spark.read.textFile(logFile).cache()
    val numAs = logData.filter(line => line.contains("a")).count()
    val numBs = logData.filter(line => line.contains("b")).count()
    println(s"Lines with a: $numAs, Lines with b: $numBs")
    */
    spark.stop()
  }
}
