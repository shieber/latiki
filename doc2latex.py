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


import sys, subprocess
import uno
import time

from unohelper import Base, systemPathToFileUrl, absolutize
import os
from os.path import splitext
from com.sun.star.beans import PropertyValue
from com.sun.star.io import XOutputStream

class OutputStream(Base,XOutputStream):
  def __init__(self):
    self.closed = 0
  def closeOutput(self):
    self.closed = 1
  def writeBytes(self,seq):
    sys.stdout.write(seq.value)
  def flush(self):
    pass

def start_OOo():
  """ Starts OOo with a listening socket. """
  if os.fork():
    return
  ooffice = "openoffice -invisible " + '"-accept=socket,host=localhost,port=2002;urp;"'
  subprocess.call(ooffice,shell=True)

def exportLaTeX(filename):
  """ Opens a document into OOo and then export it as a LaTeX file. """

  # First use some voodoo to connect to OOo
  localContext = uno.getComponentContext()
  resolver = localContext.ServiceManager.createInstanceWithContext("com.sun.star.bridge.UnoUrlResolver",localContext)
  ctx = resolver.resolve("uno:socket,host=localhost,port=2002;urp;StarOffice.ComponentContext")
  smgr = ctx.ServiceManager
  desktop = smgr.createInstanceWithContext("com.sun.star.frame.Desktop",ctx)

  # Set properties for importing and exporting
  inProps = (PropertyValue("Hidden",0,True,0),)
  outProps = (
    PropertyValue("FilterName",0,"LaTeX File",0),
    PropertyValue("Overwrite",0,True,0),
    PropertyValue("OutputStream",0,OutputStream(),0)
  )

  # Fix the pathnames for importing and exporting
  cwd = systemPathToFileUrl(os.getcwd())
  fileUrl = absolutize(cwd,systemPathToFileUrl(filename))
  (dest,ext) = splitext(filename)
  dest = dest+".tex"
  destUrl = absolutize(cwd,systemPathToFileUrl(dest))

  # Open the document then export
  doc = desktop.loadComponentFromURL(fileUrl,"_blank",0,inProps)
  doc.storeToURL(destUrl,outProps)


def doc2latex(filename):
  NoConnectionException = uno.getClass("com.sun.star.connection.NoConnectException")
  try: exportLaTeX(filename)
  except NoConnectionException:
    start_OOo()
    time.sleep(1)
    exportLaTeX(filename)

if __name__ == "__main__":
  doc2latex(sys.argv[1])
