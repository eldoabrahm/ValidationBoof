#!/usr/bin/env python3
#
# Detects QR codes for all images in a directory and saves results to a with this libraries prefix
#

if __name__ == '__main__':
    import optparse
    import os
    import sys

    script_directory = os.path.dirname(os.path.realpath(__file__))
    sys.path.append(os.path.join(script_directory,"../../../scripts"))
    from validationboof import *

    p = optparse.OptionParser()
    p.add_option('--input', '-i', default="",help="Location of directory with input images")
    p.add_option('--output', '-o', default="",help="Location of output directory results are saved to")

    options, arguments = p.parse_args()

    dir_input = os.path.abspath(options.input)
    dir_output = os.path.abspath(options.output)

    if not dir_input:
        p.print_help()
        exit(1)

    check_cd(os.path.abspath(script_directory))

    print("Looking inside of {}".format(dir_input))

    run_command("./build/zbar_benchmark -I {} -O {}".format(dir_input,dir_output))

    print( "\n\nDone!" )
