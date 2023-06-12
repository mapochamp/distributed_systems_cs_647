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

    // Count words in each document
	val trainCounts = trainTokens.flatMap { case (label, words) => words.map(word => ((label, word), 1)) }
	      .groupByKey(_._1)
	      .reduceGroups((a, b) => (a._1, a._2 + b._2))
	      .map { case ((label, word), (_, count)) => (word, (label, count)) }
	      .groupByKey(_._1)
	      .mapGroups { case (word, iter) => (word, iter.map(_._2).toMap) }

    // Convert test data to the format of (word, label)
	val testCounts = testTokens.flatMap { case (label, words) => words.map(word => (word, label)) }
      .groupByKey(_._1)
      .mapGroups { case (word, iter) => (word, iter.toList) }
	//val testCounts = testTokens.flatMap { case (label, words) => words.map(word => (word, label)) }
    //  .groupByKey(_._1)
	//  .mapGroups { case (word, iter) => (word, iter.toList.head) }



    // Make predictions on test documents
// *****************************************************************************************//
	//val predictions: Dataset[(String, Double, Double)] = testCounts.map { case (word, label) => (word, label, 1) }
	//  .join(trainCounts.map { case (word, counts) => (word, counts, 2) }, col("_1") === col("_1"))
	//  .filter(col("_3") === 1)
	//  .map { case (word, label, _, counts) =>
	//    val probSpam = Math.log(counts.getOrElse(1.0, 1).toDouble) - Math.log(counts.values.sum.toDouble)
	//    val probHam = Math.log(counts.getOrElse(0.0, 1).toDouble) - Math.log(counts.values.sum.toDouble)
	//    (word, if (probSpam > probHam) 1.0 else 0.0, label)
	//  }
// *****************************************************************************************//
	//val predictions: Dataset[(String, Double, Double)] = testCounts.map { case (word, label) => (word, label, 1) }
	//  .join(trainCounts.map { case (word, counts) => (word, counts, 2) }, col("_1") === col("_1"))
	//  .filter(col("_3") === 1)
	//  .map { row =>
	//	val word = row.getString(0)
	//	val label = row.getDouble(1)
	//	val counts = row.getMap[Double, Int](3)
	//	val probSpam = Math.log(counts.getOrElse(1.0, 1).toDouble) - Math.log(counts.values.sum.toDouble)
	//	val probHam = Math.log(counts.getOrElse(0.0, 1).toDouble) - Math.log(counts.values.sum.toDouble)
	//	(word, if (probSpam > probHam) 1.0 else 0.0, label)
	//  }
// *****************************************************************************************//

	//val predictions: Dataset[(String, Double, Double)] = testCounts.map { case (word, label) => (word, label, 1) }
	//  .toDF("word1", "label1", "id1")
	val predictions: Dataset[(String, Double, Double)] = testCounts
	  .flatMap { case (word, labels) => labels.map(label => (word, label, 1)) }
	  .toDF("word1", "label1", "id1")
	  .join(trainCounts.map { case (word, counts) => (word, counts, 2) }.toDF("word2", "counts", "id2"), col("word1") === col("word2"))
	  .filter(col("id1") === 1)
	  .map { row =>
		val word = row.getString(row.fieldIndex("word1"))
		//val label = row.getDouble(row.fieldIndex("label1"))
		val label = row.getAs[Row](row.fieldIndex("label1")).getDouble(0)
		val counts = row.getMap[Double, Int](row.fieldIndex("counts"))
		val probSpam = Math.log(counts.getOrElse(1.0, 1).toDouble) - Math.log(counts.values.sum.toDouble)
		val probHam = Math.log(counts.getOrElse(0.0, 1).toDouble) - Math.log(counts.values.sum.toDouble)
		(word, if (probSpam > probHam) 1.0 else 0.0, label)
	  }



	val accuracy = predictions.filter(row => row._2 == row._3).count().toDouble / testData.count()


    println(s"Accuracy: $accuracy")

    spark.stop()
  }

  def loadData(spark: SparkSession, directory: String, label: Double): Dataset[Email] = {
    import spark.implicits._

    val data = spark.sparkContext.wholeTextFiles(directory)
      .map { case (file, text) => Email(file, text, label) }
      .toDS()

    data
  }
}

