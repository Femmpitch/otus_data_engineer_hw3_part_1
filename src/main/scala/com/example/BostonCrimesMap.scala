package com.example

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.{collect_list, concat_ws, slice, udf}

object BostonCrimesMap extends App {

  val crimes_path = args(0)
  val offense_path = args(1)
  val output_folder = args(2)


  val spark = SparkSession
    .builder()
    .master("local[*]")
    .getOrCreate()

  import spark.implicits._


  val crimeFacts = spark
    .read
    .option("header", "true")
    .option("inferSchema", "true")
    .csv(crimes_path)


  val monthly = crimeFacts
    .groupBy("DISTRICT", "YEAR", "MONTH")
    .count()

  val offenseCodes = spark.read.option("header", "true").option("inferSchema", "true").csv(offense_path)
  val split_code_name = udf((name: String) => name.split(" - ")(0))
  val offenceCodesSplit = offenseCodes.withColumn("NAME_SPLIT", split_code_name($"NAME"))


  val codes = crimeFacts
    .join(offenceCodesSplit, $"CODE" === $"OFFENSE_CODE")
    .groupBy("DISTRICT", "NAME_SPLIT")
    .count()
    .orderBy($"DISTRICT", $"count".desc)


  val codes_most_frequent_three = codes
    .groupBy("DISTRICT")
    .agg(concat_ws(",", slice(collect_list($"NAME_SPLIT"), 1, 3)).as("frequent_crime_types"))


  monthly.createOrReplaceTempView("monthly")
  crimeFacts.createOrReplaceTempView("crime_facts")
  codes_most_frequent_three.createOrReplaceTempView("codes")


  val total = spark.sql(
    """
      |with d_m as (select monthly.DISTRICT, percentile_approx(count, 0.5) as crimes_monthly
      |             from monthly
      |             group by DISTRICT),
      |     d_a as (select crime_facts.DISTRICT, count(INCIDENT_NUMBER) as crimes_total, avg(Lat) as lat, avg(Long) as long
      |             from crime_facts
      |             group by crime_facts.DISTRICT),
      |     d_c as (select * from codes)
      |select d_a.district, d_a.crimes_total, d_m.crimes_monthly, d_c.frequent_crime_types, d_a.lat, d_a.long
      |from d_a
      |join d_m on d_a.DISTRICT = d_m.DISTRICT
      |join d_c on d_a.DISTRICT = d_c.DISTRICT
      |""".stripMargin)


  total.repartition(1).write.mode("overwrite").format("parquet").save(output_folder)

}