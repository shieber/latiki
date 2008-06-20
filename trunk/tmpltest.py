import cherry
import tempfile

cgi = cherry.Cgi()

print cgi.index("body", "title", "authors", "today", "vis", "abstract")
