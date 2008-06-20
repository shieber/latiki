#!/usr/bin/python

# This file is part of Latiki, a web-based editing and typesetting program
# Copyright (C) 2008 Joseph Barillari & Tim Credo

# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.

# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.

# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

"""

Take an HTTP post of a LaTeX file, return a PDF

"""

import cgitb; cgitb.enable()

import tempfile, subprocess, os, cgi, sys

def main():
    """ run app """
    fst = cgi.FieldStorage()
    tex = fst.getfirst("tex")
    if not tex or tex.strip() == "":
        raise Exception
    else:
        dirname = tempfile.mkdtemp()
        os.chdir(dirname)
        texname = os.path.join(dirname, "file.tex")
        open(texname,'w').write(tex)

        process = subprocess.Popen(["pdflatex", r"\nonstopmode\input", texname],
                         stdout=subprocess.PIPE,
                         stderr=subprocess.STDOUT)
        output = process.communicate()[0]
        if process.returncode != 0:
            print """Content-Type: text/plain"""
            print
            print
            print output
        else:
            print """Content-Type: application/pdf"""
            print
            print
            sys.stdout.write(open(os.path.join(dirname, "file.pdf")).read())
            



main()


