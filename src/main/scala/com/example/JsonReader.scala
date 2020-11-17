import org.apache.spark.sql.SparkSession
import org.json4s.jackson.JsonMethods.parse

object JsonReader extends App{
  class JsonReaderClass (file_path: String) {


    var fp: String = file_path

    val spark = SparkSession
      .builder()
      //.master("local[*]")
      .getOrCreate()

    val sc = spark.sparkContext

    val lines = sc.textFile(fp)

    case class WineRecord (id: Any, country: Any, points: Any, title: Any, variety: Any, winery: Any)

    val parsed = lines
      .map (s => parse(s).values.asInstanceOf[Map[String, Any]])
      .map (s => s.map{ case (k, v) => k -> (if (s.contains(k)) v else -1) })

    parsed.collect().foreach(s => println(WineRecord(s.getOrElse("id", None), s.getOrElse("country", None),
      s.getOrElse("points", None), s.getOrElse("title", None), s.getOrElse("variety", None),
      s.getOrElse("winery", None))))
  }

  val file_path = args(0)
  val reader = new JsonReaderClass(file_path)
}
