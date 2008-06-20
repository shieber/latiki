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


from difflib import SequenceMatcher

def versionCompare(seq1,seq2):
  opcodes = SequenceMatcher(lambda x: 0,seq1,seq2).get_opcodes()
  diff = []
  for tag,i1,i2,j1,j2 in opcodes:
    if tag == "equal":
      diff.append((0,seq1[i1:i2]))
    elif tag == "delete":
      diff.append((1,seq1[i1:i2]))
    elif tag == "insert":
      diff.append((2,seq2[j1:j2]))
    elif tag == "replace":
      diff.append((1,seq1[i1:i2]))
      diff.append((2,seq2[j1:j2]))
  return diff

