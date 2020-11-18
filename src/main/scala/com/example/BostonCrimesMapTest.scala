package com.example

import org.apache.spark.sql.SparkSession

object BostonCrimesMapTest extends App{

  val spark = SparkSession
    .builder()
    .master("local[*]")
    .getOrCreate()

  val df = spark.read.parquet("../data/boston/output_spark")
  df.show
}
