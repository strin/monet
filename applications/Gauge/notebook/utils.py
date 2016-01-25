import subprocess
import plotly.plotly as py
import plotly.graph_objs as go
import re

device_id = '03157df3929b6037'

def run_cmd(cmd):
    p = subprocess.Popen(cmd.split(' '),
            stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    out, err = p.communicate()
    return out

def parse_table(out, split_ch=' '):
    vs = dict()

    lines = out.split('\n')
    for line in lines:
        items = line.strip().split(split_ch)
        if len(items) % 2 != 0:
            continue
        for i in range(0, len(items), 2):
            name = items[i]
            val = float(items[i+1])
            if name not in vs:
                vs[name] = []
            vs[name].append(val)

    return vs

def get_runs():
    out = run_cmd('adb -s %s shell ls -l /sdcard/*.txt' % device_id)
    print 'output', out
    run_ids = set(re.findall('.*/([0-9]*)\.txt[\r]?\n', out))
    return list(run_ids)

def get_vs(run_id, experiment_id):
    out = run_cmd('adb -s %s shell cat /sdcard/%s_%s.txt' % (device_id, run_id, experiment_id))
    vs = parse_table(out)
    return vs

def plot_xy(x, y, names=None, xlabel='x', ylabel='y', title=''):

    layout = go.Layout(
        title=title,
        xaxis=dict(
            title=xlabel,
            titlefont=dict(
                family='Courier New, monospace',
                size=18,
                color='#7f7f7f'
            )
        ),
        yaxis=dict(
            title=ylabel,
            titlefont=dict(
                family='Courier New, monospace',
                size=18,
                color='#7f7f7f'
            )
        )
    )
    
    if type(x) == list:
        xs = x
        ys = y
        if not names:
            names = ['line-' + i for i in range(len(xs))]
    else:
        xs = [x]
        ys = [y]
        names = ['line']
    
    traces = []

    for (x, y, name) in zip(xs, ys, names):
        trace = go.Scatter(
            x = x,
            y = y,
            name = name
        )
        traces.append(trace)

    data = traces
    fig = go.Figure(data=data, layout=layout)
    disp = py.iplot(fig, filename='basic-line')
    return disp.data








