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

""" simple web server for tex converter """

# FIXME: update this for your setup
BASEDIR = "/home/latiki/dogfood"
DATADIR = BASEDIR + "/data/"
TEMPLATESDIR = BASEDIR + "/templates/"

L_KEY_PREFIX = "__L__"
L_KEY_USER = L_KEY_PREFIX + "USER"
L_KEY_DATE = L_KEY_PREFIX + "DATE"


REQSDIRNAME = "reqs"
from Cheetah.Template import Template

import os, sys, subprocess, tempfile, time, shutil, urllib, re
import cPickle

sys.path.append('cpy_hack/build/lib')
import cherrypy, simplejson
safeNameRe = re.compile("^[a-zA-Z0-9_.-]+$")

WCDIR = "wc"
REPODIR = "repo"

from doc2latex import doc2latex


UNSAFE_STRINGS = [r'\include', r'\input']

STYLES = ["IEEE", "generic", "Elsevier1", "Elsevier2", "AMS"]

class HelloWorld:
    """ root """
    @cherrypy.expose
    def index(self):
        return open('hello.html').read()

# def fileListInner(client, start, relPathIndex):
#     ret = []

#     for item in client.ls(start):
#         leafName = item['name'][item['name'].rfind('/')+1:]
#         path = item['name'][relPathIndex:]
#         if item['kind'] == pysvn.node_kind.file:
#             ret.append( ('file', leafName, path,  []))
#         elif item['kind'] == pysvn.node_kind.dir:
#             ret.append( ('dir', leafName, path,
#                          fileListInner(client, item['name'], relPathIndex)))
#         else:
#             assert 0
#     return ret

def getRevisionData(repoid, revnum):
    """
    return the dict of a revision
    """
    assert safeNameRe.match(repoid)
    assert type(revnum) == type(0) or safeNameRe.match(revnum)
    return cPickle.load(open(os.path.join(DATADIR, repoid, str(revnum))))

def saveNewRevision(repoid, data, date, user):
    """
    FIXME! we should do some sort of locking here...
    """
    for key in [k for k in data.keys()]:
        # strip all metadata that the user may have inserted
        if key.find(L_KEY_PREFIX) > -1:
            del data[key]
    data[L_KEY_DATE] = date
    data[L_KEY_USER] = user
    
    vnum = getLatestRevisionNumber(repoid) + 1
    assert safeNameRe.match(repoid)
    target = open(os.path.join(DATADIR, repoid, str(vnum)), "w")
    cPickle.dump(data, target)
    return vnum


def getLatestRevisionNumber(repoid):
    """
    return the latest revision of the repo
    """
    repodir = os.path.join(DATADIR, repoid)
    assert os.path.exists(repodir)
    mnum = -1
    for fname in os.listdir(repodir):
        vnum = int(fname)
        if vnum > mnum:
            mnum = vnum
    return mnum

def dataEqual(data1, data2):
    """
    compare to data dicts for equality. ignore all Latiki-use-only
    keys
    """
    d1keys = [key for key in data1.keys() if key.find(L_KEY_PREFIX) == -1]
    d2keys = [key for key in data2.keys() if key.find(L_KEY_PREFIX) == -1]
    if len(d1keys) != len(d2keys):
        return False
    for key in d1keys:
        if not key in d2keys:
            return False
    for key in d1keys:
        if not data1[key] == data2[key]:
            return False
    return True
    
class RepoStuff:
    """ repo manipulation """
    @cherrypy.expose
    def save(self, repoid, data, currentRevision, user):
        """ attempt to save a new revision of the document """
        assert safeNameRe.match(user)
        error = "incomplete run"
        result = {}
        now = time.strftime("%Y/%m/%d %H:%M:%S", time.gmtime())
        decoder = simplejson.JSONDecoder()
        data = decoder.decode(data)
        latest = getLatestRevisionNumber(repoid)
        if latest > currentRevision:
            result['state'] = "conflict"
        else:
            if latest > -1 and dataEqual(data, getRevisionData(repoid, latest)):
                result['state'] = "nochange"
            else:
                result['state'] = "new"
                result['newrevision'] = str(saveNewRevision(repoid, data, now, user))
                result['date'] = now
                result['user'] = user
            error = None
        encoder = simplejson.JSONEncoder()
        # why parens? because it doesn't work otherwise.
        return "(" + encoder.encode({"result": result, "error": error}) + ")"


    @cherrypy.expose
    def compareRevisions(self, repoid, fromrev, torev):
        """
        returns a JSON object comparing the two revs, or "failed" otherwise.
        note that for now this just compares the body text
        """
        fdata = getRevisionData(repoid, fromrev)['body']        
        tdata = getRevisionData(repoid, torev)['body']        
        import versionCompare
        output = versionCompare.versionCompare(fdata, tdata)
        return simplejson.dumps(output)

    @cherrypy.expose
    def listRevisions(self, repoid):
        """
        list the previous revisions
        """
        repodir = os.path.join(DATADIR, repoid)
        assert os.path.exists(repodir)

        encoder = simplejson.JSONEncoder()
        vlist = [int(ver) for ver in os.listdir(repodir)]
        vlist.sort()

        out = []
        for vnum in vlist:
            data = getRevisionData(repoid, vnum)
            out.append( [ str(vnum),
                          data[L_KEY_DATE],
                          data[L_KEY_USER] ] )
        return encoder.encode(out)

    @cherrypy.expose
    def makeRepository(self, repoid):
        """
        creates a repository if it does not yet exist.
        """
        if not safeNameRe.match(repoid):
            return "fail"
        repodir = os.path.join(DATADIR, repoid)
        if not os.path.exists(repodir):
            try:
                os.mkdir(repodir)
                return "new"
            except:
                return "fail"
        return "exists"
        
    @cherrypy.expose
    def getRevision(self, repoid, revision=None):
        """
        get a revision as a JSON object
        """
        repodir = os.path.join(DATADIR, repoid)
        assert os.path.exists(repodir)
        if revision:
            assert str(int(revision)) == revision # defend against non-ints
        else:
            revision = getLatestRevisionNumber(repoid)
        encoder = simplejson.JSONEncoder()
        data = getRevisionData(repoid, revision)
        data['revnum'] = str(revision)
        return encoder.encode(data)
        
#    @cherrypy.expose
#     def listVersions(self, repoid, path):
#         encoder = simplejson.JSONEncoder()
#         assert repoid == "demopaper1"
#         assert path[0] == "/"
#         filepath = os.path.join(DATADIR, repoid, WCDIR, path[1:])
#         client = pysvn.Client()
#         out = []
#         for entry in client.log(filepath):
#             out.append( [ str(entry['revision'].number),
#                           time.strftime("%Y/%m/%d %H:%M:%S", time.gmtime(entry['date'])),
#                           entry['author'] ] )
#         return encoder.encode(out)
        

#     @cherrypy.expose
#     def filelist(self, repoid):
#         encoder = simplejson.JSONEncoder()
#         assert repoid == "demopaper1"
#         wcdir = os.path.join(DATADIR, repoid, WCDIR)
#         return encoder.encode(fileListInner(pysvn.Client(), wcdir, len(wcdir)))

#     @cherrypy.expose
#     def loadText(self, repoid, path, version):
#         client = pysvn.Client()
#         assert repoid == "demopaper1"
#         assert path[0] == "/"
#         assert version == "current"
#         wcpath = os.path.join(DATADIR, repoid, WCDIR, path[1:])
#         return client.cat(wcpath)


#     @cherrypy.expose
#     def saveText(self, repoid, path, text):
#         client = pysvn.Client()
#         assert repoid == "demopaper1"
#         assert path[0] == "/"
#         wcdir = os.path.join(DATADIR, repoid, WCDIR)
#         # do an update
#         client.revert(wcdir)
#         client.update(wcdir)
#         open(os.path.join(DATADIR, repoid, WCDIR, path[1:]),'w').write(text)
#         return client.checkin(wcdir, "web revision")

#     @cherrypy.expose
#     def makePDF(self, repoid):
#         client = pysvn.Client()
#         assert repoid == "demopaper1"
#         client.revert(wcdir)
#         client.update(wcdir)
#         wcdir = os.path.join(DATADIR, repoid, WCDIR)
#         process = subprocess.Popen(["make"],
#                                        cwd = wcdir,
#                                        stdout = subprocess.PIPE,
#                                        stderr=subprocess.STDOUT)
#         output = process.communicate()[0]
#         if process.returncode != 0:
#             cherrypy.response.headers['Content-Type'] = "text/plain"
#             return output
#         else:
#             cherrypy.response.headers['Content-Type'] = "application/pdf"
#             pdf =  open(os.path.join(dirname, "file.pdf")).read()
#             client.revert(wcdir)
#             return pdf
        

class Upload:
    """ upload """
    @cherrypy.expose
    def index(self, docfile=None):
        """ convert doc to latex """
        content = None
        cpgre = re.compile(r'\\clearpage.*')        
        if docfile != None:
            # insert file procesing code here
            dirname = tempfile.mkdtemp()
            docname = os.path.join(dirname, docfile.filename)
            open(docname,'w').write(docfile.file.read())
            doc2latex(docname)
            texname = os.path.splitext(docname)[0]+".tex"
            latex = open(texname,'r').read()
            latex = cpgre.sub("", latex)
            latex = latex.replace(r'\textquotesingle',"'")
            content = latex.replace(r'\endinput','')
            content = urllib.quote(content)

        output = str(Template(file = open(os.path.join(BASEDIR, "upload.html")),
                            searchList = [{"content": content}]))
        return output

class Cgi:
    """ cgi """
    @cherrypy.expose
    def index(self, body, title, authors, date, style, abstract):
        if not body or body.strip() == "":
            raise cherrypy.HTTPError(400, "tex required")
        else:
            #for uss in UNSAFE_STRINGS:
            #    if body.find(uss) > -1:
            #        raise cherrypy.HTTPError(400, r"\include, etc. are not ok")

            params = { 'body': body,
                       'title': title,
                       'authors': authors,
                       'date': date,
                       'abstract': abstract }

            assert style in STYLES
            templatedir = os.path.join(TEMPLATESDIR, style)
            tex = str(Template(file=open(os.path.join(templatedir, "template.tex")),
                         searchList=[params]))

            dirname = tempfile.mkdtemp()
            texname = os.path.join(dirname, "file.tex")

            open(texname,'w').write(tex)

            # copy all of the requisites
            reqsdir = os.path.join(templatedir, REQSDIRNAME)
            for req in os.listdir(reqsdir):
                shutil.copyfile(os.path.join(reqsdir, req), os.path.join(dirname, req))
            
            process = subprocess.Popen(#["pdflatex", r"\nonstopmode\input", texname],
                ["rubber", "-d", texname],
                                       cwd = dirname,
                                       stdout = subprocess.PIPE,
                                       stderr=subprocess.STDOUT)
            output = process.communicate()[0]
            if process.returncode != 0:
                cherrypy.response.headers['Content-Type'] = "text/plain"
                return output
            else:
                cherrypy.response.headers['Content-Type'] = "application/pdf"
                return open(os.path.join(dirname, "file.pdf")).read()
        

def main():
    """ main """
    root = HelloWorld()

    root.cgi = Cgi()
    root.upload = Upload()
    root.repo = RepoStuff()    

    conf = {'/': {'tools.staticdir.root': os.path.join(BASEDIR, 'static/')},
            '/www' : {'tools.staticdir.on' : True,
                         'tools.staticdir.dir' : "www"}}
    cherrypy.server.socket_port = 8112
#    cherrypy.server.socket_host = "127.0.0.1"
    cherrypy.quickstart(root, config=conf)


if __name__ == '__main__':
    main()
