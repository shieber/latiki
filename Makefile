all:
	(cd cpy_hack; python setup.py build)
	(cd static; ./Latiki-compile)