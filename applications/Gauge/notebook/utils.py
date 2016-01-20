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

def get_time_energy(run_id):
    out = run_cmd('adb -s %s shell cat /sdcard/%s_energy_time.txt' % (device_id, run_id))
    vs = parse_table(out)
    return (vs['time'], vs['energy'])

def plot_xy(x, y):
    trace = go.Scatter(
        x = x,
        y = y,
    )

    data = [trace]

    disp = py.iplot(data, filename='basic-line')
    return disp.data








