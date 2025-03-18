**File Tree Hashes Comparer**

Stores and compares hashes of directories and its content.
Each time it runs it compares current hashes to previously stored hashes for changes.
Especially useful for backups on filesystems which are not check summed like NTFS, APFS.
So you can detect when file changed.

Stores hashes in CSV format in file `.hashes.csv` in selected directory.

Calculate, compare, stores hashes for a given directory:
```
java -jar MyJar.jar "/Users/X/Documents"
```

Calculate hash for a single directory:
```
java -jar MyJar.jar "/Users/X/Documents/Z.mp3"
```
