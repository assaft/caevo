#!/usr/bin/python
import re
import os
from github3 import login, GitHub
from github3 import ForbiddenError
from collections import defaultdict


htmlfile = 'timeml_report.html'
introfile = 'timeml_report.txt'

user = 'assaft'
repository = 'caevo'

columns = ['id', 'text', 'timeml', 'gold', 'expected', 'scores', 'comments']

elements = ['events', 'times', 'links']

def writefile(outfile,fname,table,list,name):
    #print (list)
    
    if name == 'scores':
        with open(fname, 'r') as infile:
            outfile.write('<table class=\'table sub\'>\n\n')
            lineId = 0
            for line in infile:
                if (not line.isspace()):
                    line = line.rstrip()
                    cols = line.split(',')
                    tag = ""
                    if lineId == 0:
                        tag = 'th'
                    else:
                        tag = 'td'
                    outfile.write('<tr>')
                    for col in cols:
                        outfile.write('<' + tag + ' class="padder score-name-col" valign="top">' + col + '</' + tag + '>')
                    outfile.write('<tr>')
                lineId = lineId + 1    
            outfile.write('</table>\n<br>\n')    
    else:
        with open(fname) as infile:
            if (table):
                outfile.write('<table class=\'table sub\'>\n\n')
            else:
                outfile.write('<div class="marginer">')
            for line in infile:
                if (not line.isspace()):
                    line = line.rstrip()
                    if (table):
                        element = line.split(',')[0]
                        #print (element + ':' + str(element in list))
                        style=name + '-'                        
                        num = ""
                        if (element in list):
                            style = style +'match'
                            num = str(list.index(element))
                        else:
                            style = style + 'nomatch'
                            num = ""
                        line = '<tr><td name=\'' + name + '\' class=\'td sub '+style+'\' valign="top">\n' + line + '</td>\n<td name=\'' + name + '\' class=\'td index sub '+style+'\' valign="top">\n'+num+'</td>\n</tr>\n'
                    outfile.write(line)
                    if (not table):
                        outfile.write('<br>')
            if (table):
                outfile.write('</table>\n')
            else:
                outfile.write('</div>\n')
                
    return;


def elementId(argument):
    switcher = {
        0:  'events',
        1:  'times',
        2:  'links',
    }
    return switcher.get(argument)    
    
def extension(argument):
    switcher = {
        'timeml':   'stf',
        'gold':     'stf',
        'expected': 'stf',
        'scores':   'csv',
        'comments': 'txt',
    }
    return switcher.get(argument, "err")    

def mandatory(argument):
    switcher = {
        'id':       True,
        'text':     True,
        'timeml':   True,
        'gold':     True,
        'expected': True,
        'scores':   False,
        'comments': False,
    }
    return switcher.get(argument, True)

def subtable(argument):
    switcher = {
        'id':       False,
        'text':     False,
        'timeml':   True,
        'gold':     True,
        'expected': True,
        'scores':   True,
        'comments': False,
    }
    return switcher.get(argument, True)
    
def issues():
    testToIssue = {}
    try:
        # user = GitHub("<user>",token="<token>")
        user = GitHub()
        issues = user.issues_on("assaft", "caevo",state="open")
        regex = r"\btest\b ([tT]\d+)"
        group = 1
        for issue in issues:
            text = "Issue by " + issue.user.login + ":\n" + issue.title + "\n" + issue.body + "\n\n"
            for comment in issue.comments():
                text = text + "commit by " + comment.user.login + ":\n" + comment.body + "\n\n"

            matches = re.finditer(regex, text)
            for match in matches:
                test = match.group(group)
                #print test
                if (test not in testToIssue):
                    testToIssue[test] = set()
                testIssues = testToIssue[test]
                testIssues.add(issue.number)
    except ForbiddenError as err:
        print ('Issues cannot be fetched from GitHub because connection as an anonymous user has failed. Wait for GitHub counter to reset or put your username and password/token in the script. Message from GitHub:\n' + format(err)); 
    except:
        sys.exit('Unexpected error fetching issues from GitHub')

    return testToIssue;    

def link(id):
    return '<a href=https://github.com/assaft/caevo/issues/'+str(id)+'>#'+str(id)+'</a>'

def scripts():
    return  """<script>\n

                function toggleByName(name) { 
                    var x = document.getElementsByName(name);
                    var i;
                    for (i = 0; i < x.length; i++) {
                        x[i].classList.toggle("unmark");
                    }
                }
                
                function toggleFunction() { 
                    toggleByName('gold');
                    toggleByName('expected');
                }
                
                </script>\n"""

def toggle():
    # code and css were generated by: https://proto.io/freebies/onoff/ 
    return  """ <div class="onoffswitch">
                    <input type="checkbox" name="onoffswitch" class="onoffswitch-checkbox" onclick="toggleFunction()" id="myonoffswitch" checked>
                    <label class="onoffswitch-label" for="myonoffswitch">
                        <span class="onoffswitch-inner"></span>
                        <span class="onoffswitch-switch"></span>
                    </label>
                </div>"""

    
print ("Creating TimeML Test Suite report file: " + htmlfile)

with open(htmlfile, 'w') as outfile:
    outfile.write('<!DOCTYPE html>\n<html>\n')
    outfile.write('<head>\n<link rel="stylesheet" type="text/css" href="timeml_report.css">\n</head>\n')
    outfile.write('<body>\n')
    
    outfile.write(scripts())
    
    # put the intro file
    with open(introfile, 'r') as f:
        #print ('Adding ' + introfile)
        for i, x in enumerate(f):
            if i == 0:
                outfile.write('<h1 align="center">'+x.rstrip()+'</h1>\n')
            else:
                outfile.write('<p>'+x.rstrip()+'</p>\n')
    outfile.write('\n\n')
    
    # put the switch toggle
    outfile.write(toggle())
    
    # collect the issues mentioning the tests
    issues = issues()

    # create the table for adding the tests row-by-row
    outfile.write('<table style="width:100%">\n<tr>\n')
    for name in columns:
        outfile.write('<th class=\'' + name + '-col\'>' + name.title() + '</th>\n')
    outfile.write('</tr>\n')

    
    filelist = sorted(os.listdir("."))
    #filelist = ["t01.txt"]
    for tname in filelist:
        matchObj  = re.match(r'^t\d\d\.txt$', tname)
        if (matchObj):
            outfile.write('<tr>\n')
            
            # create a list which holds the items to look for in each column
            # we only fill the lists of the 'gold' and 'expected', so they are
            # the only ones to get painted in case of a match or vice versa.
            lists = defaultdict(list)
            fname = os.path.splitext(tname)[0] + '_alignment.txt'
            if (os.path.isfile(fname)):
                with open(fname) as infile:
                    for line in infile:
                        if (not line.isspace()):
                            elements = line.rstrip().split('=')
                            lists['gold'].append(elements[0])
                            lists['expected'].append(elements[1])
                        
            for name in columns:
                outfile.write('<td valign="top">\n')
                title = ''
                fname = ''
                if name == 'id':
                    outfile.write(tname[1:3])
                else:
                    if name == 'text':
                        fname = tname
                    else:
                        fname = os.path.splitext(tname)[0] + '_'+name+'.'+extension(name)
      
                    fileAdded = False
                    if mandatory(name) or os.path.isfile(fname):
                        #print ('Adding ' + fname)
                        writefile(outfile,fname,subtable(name),lists[name],name)
                        fileAdded = True
                        
                    if name == 'comments':
                        test = 't'+tname[1:3]
                        if test in issues:
                            #print ('Including issues for test ' + test)
                            if fileAdded:
                                outfile.write('<br>')
                            outfile.write('<div class="marginer">Open issue(s): ' + ','.join(link(x) for x in issues[test]) + '</div>')
                
                outfile.write('</td>\n')
            outfile.write('</tr>\n')
    outfile.write('</table>\n')

                    
    outfile.write('</body>\n</html>\n')

print ('Done.')
