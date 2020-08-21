# Spark Diff

Add the following `import` to your code:
```scala
import uk.co.gresearch.spark.diff._
```

This adds a `diff` transformation to `Dataset` that computes the differences between two datasets,
i.e. which rows of `this` dataset to _add_, _delete_ or _change_ to get to the given dataset.

For example, with
```scala
val left = Seq((1, "one"), (2, "two"), (3, "three")).toDF("id", "value")
val right = Seq((1, "one"), (2, "Two"), (4, "four")).toDF("id", "value")
```
diffing becomes as easy as:
```scala
left.diff(right).show()
```
|diff |id   |value  |
|:---:|:---:|:-----:|
|    N|    1|    one|
|    D|    2|    two|
|    I|    2|    Two|
|    D|    3|  three|
|    I|    4|   four|

With columns that provide unique identifiers per row (here `id`), the diff looks like:
```scala
left.diff(right, "id").show()
```
|diff |id   |left_value|right_value|
|:---:|:---:|:--------:|:---------:|
|    N|    1|       one|        one|
|    C|    2|       two|        Two|
|    D|    3|     three|     *null*|
|    I|    4|    *null*|       four|


Equivalent alternative is this hand-crafted transformation
```scala
left.withColumn("exists", lit(1)).as("l")
  .join(right.withColumn("exists", lit(1)).as("r"),
    $"l.id" <=> $"r.id",
    "fullouter")
  .withColumn("diff",
    when($"l.exists".isNull, "I").
      when($"r.exists".isNull, "D").
      when(!($"l.value" <=> $"r.value"), "C").
      otherwise("N"))
  .show()
```

Statistics on the differences can be obtained by
```scala
left.diff(right, "id").groupBy("diff").count().show()
```

|diff  |count  |
|:----:|:-----:|
|     N|      1|
|     I|      1|
|     D|      1|
|     C|      1|

The `diff` transformation can optionally provide a *change column* that lists all non-id column names that have changed.
This column is an array of strings and only set for `"N"` and `"C"`action rows; it is *null* for `"I"` and `"D"`action rows.

|diff |changes|id   |left_value|right_value|
|:---:|:-----:|:---:|:--------:|:---------:|
|    N|     []|    1|       one|        one|
|    C|[value]|    2|       two|        Two|
|    D| *null*|    3|     three|     *null*|
|    I| *null*|    4|    *null*|       four|

## Features

This `diff` transformation provides the following features:
* id columns are optional
* provides typed `diffAs` transformations
* supports *null* values in id and non-id columns
* detects *null* value insertion / deletion
* configurable via `DiffOptions`:
  * diff column name (default: `"diff"`), if default name exists in diff result schema
  * diff action labels (defaults: `"N"`, `"I"`, `"D"`, `"C"`), allows custom diff notation,
e.g. Unix diff left-right notation (<, >) or git before-after format (+, -, -+)
* optionally provides a *change column* that lists all non-id column names that have changed (only for `"D"` action rows)

## Configuring Diff

Diffing can be configured via an optional `DiffOptions` instance (see [Methods](#methods) below).

|option              |default  |description|
|--------------------|:-------:|-----------|
|`diffColumn`        |`"diff"` |The 'diff column' provides the action or diff value encoding if the respective row has been inserted, changed, deleted or has not been changed at all.|
|`leftColumnPrefix`  |`"left"` |Non-id columns of the 'left' dataset are prefixed with this prefix.|
|`rightColumnPrefix` |`"right"`|Non-id columns of the 'right' dataset are prefixed with this prefix.|
|`insertDiffValue`   |`"I"`    |Inserted rows are marked with this string in the 'diff column'.|
|`changeDiffValue`   |`"C"`    |Changed rows are marked with this string in the 'diff column'.|
|`deleteDiffValue`   |`"D"`    |Deleted rows are marked with this string in the 'diff column'.|
|`nochangeDiffValue` |`"N"`    |Unchanged rows are marked with this string in the 'diff column'.|
|`changeColumn`      |*none*   |An array with the names of all columns that have changed values is provided in this column (only for unchanged and changed rows, *null* otherwise).|

Either construct an instance via the constructor …

```scala
val options = DiffOptions("d", "l", "r", "i", "c", "d", "n", Some("changes"))
```

… or via the `.with*` methods. The former requires all options to be specified, whereas the latter
only requires the ones that deviate from the default, and it is more readable.

Start from the default options `DiffOptions.default` and customize as follows:

```scala
val options = DiffOptions.default
  .withDiffColumn("d")
  .withLeftColumnPrefix("l")
  .withRightColumnPrefix("r")
  .withInsertDiffValue("i")
  .withChangeDiffValue("c")
  .withDeleteDiffValue("d")
  .withNochangeDiffValue("n")
  .withChangeColumn("changes")
```

## Methods

* `def diff(other: Dataset[T], idColumns: String*): DataFrame`
* `def diff(other: Dataset[T], options: DiffOptions, idColumns: String*): DataFrame`


* `def diffAs[U](other: Dataset[T], idColumns: String*)(implicit diffEncoder: Encoder[U]): Dataset[U]`
* `def diffAs[U](other: Dataset[T], options: DiffOptions, idColumns: String*)(implicit diffEncoder: Encoder[U]): Dataset[U]`
* `def diffAs[U](other: Dataset[T], diffEncoder: Encoder[U], idColumns: String*): Dataset[U]`
* `def diffAs[U](other: Dataset[T], options: DiffOptions, diffEncoder: Encoder[U], idColumns: String*): Dataset[U]`
