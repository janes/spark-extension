/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.co.gresearch.spark

import org.apache.spark.sql.{DataFrame, Dataset, Encoder}

package object diff {

  implicit class DatasetDiff[T](ds: Dataset[T]) {

    /**
     * Returns a new DataFrame that contains the differences between this and the other Dataset
     * of the same type `T`. Both Datasets must contain the same set of column names and data types.
     * The order of columns in the two Datasets is not important as one column is compared to the
     * column with the same name of the other Dataset, not the column with the same position.
     *
     * Optional id columns are used to uniquely identify rows to compare. If values in any non-id
     * column are differing between this and the other Dataset, then that row is marked as `"C"`hange
     * and `"N"`o-change otherwise. Rows of the other Dataset, that do not exist in this Dataset
     * (w.r.t. the values in the id columns) are marked as `"I"`nsert. And rows of this Dataset, that
     * do not exist in the other Dataset are marked as `"D"`elete.
     *
     * If no id columns are given, all columns are considered id columns. Then, no `"C"`hange rows
     * will appear, as all changes will exists as respective `"D"`elete and `"I"`nsert.
     *
     * The returned DataFrame has the `diff` column as the first column. This holds the `"N"`, `"C"`,
     * `"I"` or `"D"` strings. The id columns follow, then the non-id columns (all remaining columns).
     *
     * {{{
     *   val df1 = Seq((1, "one"), (2, "two"), (3, "three")).toDF("id", "value")
     *   val df2 = Seq((1, "one"), (2, "Two"), (4, "four")).toDF("id", "value")
     *
     *   df1.diff(df2).show()
     *
     *   // output:
     *   // +----+---+-----+
     *   // |diff| id|value|
     *   // +----+---+-----+
     *   // |   N|  1|  one|
     *   // |   D|  2|  two|
     *   // |   I|  2|  Two|
     *   // |   D|  3|three|
     *   // |   I|  4| four|
     *   // +----+---+-----+
     *
     *   df1.diff(df2, "id").show()
     *
     *   // output:
     *   // +----+---+----------+-----------+
     *   // |diff| id|left_value|right_value|
     *   // +----+---+----------+-----------+
     *   // |   N|  1|       one|        one|
     *   // |   C|  2|       two|        Two|
     *   // |   D|  3|     three|       null|
     *   // |   I|  4|      null|       four|
     *   // +----+---+----------+-----------+
     *
     * }}}
     *
     * The id columns are in order as given to the method. If no id columns are given then all
     * columns of this Dataset are id columns and appear in the same order. The remaining non-id
     * columns are in the order of this Dataset.
     *
     * @group untypedrel
     */
    @scala.annotation.varargs
    def diff(other: Dataset[T], idColumns: String*): DataFrame = {
      Diff.of(this.ds, other, idColumns: _*)
    }

    /**
     * Returns a new DataFrame that contains the differences
     * between this and the other Dataset of the same type `T`.
     *
     * See `diff(Dataset[T], String*`.
     *
     * The schema of the returned DataFrame can be configured by the given `DiffOptions`.
     *
     * @group untypedrel
     * @since 3.0.0
     */
    @scala.annotation.varargs
    def diff(other: Dataset[T], options: DiffOptions, idColumns: String*): DataFrame = {
      new Diff(options).of(this.ds, other, idColumns: _*)
    }

    /**
     * Returns a new Dataset that contains the differences
     * between this and the other Dataset of the same type `T`.
     *
     * See `diff(Dataset[T], String*`.
     *
     * This requires an additional implicit `Encoder[U]` for the return type `Dataset[U]`.
     *
     * @group typedrel
     * @since 3.0.0
     */
    @scala.annotation.varargs
    def diffAs[U](other: Dataset[T], idColumns: String*)
                 (implicit diffEncoder: Encoder[U]): Dataset[U] = {
      Diff.ofAs(this.ds, other, idColumns: _*)
    }

    /**
     * Returns a new Dataset that contains the differences
     * between this and the other Dataset of the same type `T`.
     *
     * See `diff(Dataset[T], String*)`.
     *
     * This requires an additional implicit `Encoder[U]` for the return type `Dataset[U]`.
     * The schema of the returned Dataset can be configured by the given `DiffOptions`.
     *
     * @group typedrel
     * @since 3.0.0
     */
    @scala.annotation.varargs
    def diffAs[U](other: Dataset[T], options: DiffOptions, idColumns: String*)
                 (implicit diffEncoder: Encoder[U]): Dataset[U] = {
      new Diff(options).ofAs(this.ds, other, idColumns: _*)
    }

    /**
     * Returns a new Dataset that contains the differences
     * between this and the other Dataset of the same type `T`.
     *
     * See `diff(Dataset[T], String*`.
     *
     * This requires an additional explicit `Encoder[U]` for the return type `Dataset[U]`.
     *
     * @group typedrel
     * @since 3.0.0
     */
    @scala.annotation.varargs
    def diffAs[U](other: Dataset[T], diffEncoder: Encoder[U], idColumns: String*): Dataset[U] = {
      Diff.ofAs(this.ds, other, idColumns: _*)(diffEncoder)
    }

    /**
     * Returns a new Dataset that contains the differences
     * between this and the other Dataset of the same type `T`.
     *
     * See `diff(Dataset[T], String*`.
     *
     * This requires an additional explicit `Encoder[U]` for the return type `Dataset[U]`.
     * The schema of the returned Dataset can be configured by the given `DiffOptions`.
     *
     * @group typedrel
     * @since 3.0.0
     */
    @scala.annotation.varargs
    def diffAs[U](other: Dataset[T],
                  options: DiffOptions,
                  diffEncoder: Encoder[U],
                  idColumns: String*): Dataset[U] = {
      new Diff(options).ofAs(this.ds, other, diffEncoder, idColumns: _*)
    }
  }

}
