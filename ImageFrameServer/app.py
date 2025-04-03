from flask import Flask, request
import subprocess, time

app = Flask(__name__)

last_run_time = 0  # global timestamp

@app.route('/run')
def run_script():
    global last_run_time
    now = time.time()

    # Check if 30 seconds have passed
    if now - last_run_time < 30:
        return "Too soon. Please wait before running again.", 429  # HTTP 429 Too Many Requests

    last_run_time = now

    assetId = request.args.get('assetId', '')
    
    print(assetId)

    result = subprocess.run(
        ["python3", "frame_script.py", assetId],
        capture_output=True, text=True
    )
    return f"<pre>{result.stdout}</pre>"

@app.route('/status', methods=['GET'])
def first_status():
    return "OK"

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)