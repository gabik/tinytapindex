#pylint: disable=missing-module-docstring, missing-function-docstring, invalid-name
import json
import requests
from flask import Flask, render_template, jsonify, request
app = Flask(__name__)

@app.route('/')
def root():
    return render_template('test.html')

@app.route('/privicy')
def privicy():
    return render_template('privicy.html')

@app.route('/get_content', methods=['POST'])
def get_content():
    args = request.form
    print(args)
    url = "https://www.tinytap.it/store/api/content/category/{cat}/?language={lan}&ageGroup={age}&ver=3.4&page_num={pn}&per_page=1000" #pylint: disable=line-too-long
    data = []
    more_data = True
    pn = 1
    while more_data:
        full_url = url.format(cat=args['cat'], age=args["age"], lan=args["lan"], pn=pn)
        print("Getting {}".format(full_url))
        cur_raw = requests.get(full_url)
        pn += 1
        if cur_raw.status_code == 200:
            cur_data = json.loads(cur_raw.text)
            if cur_data['result'] == 'success' and cur_data['data']:
                for cont in cur_data['data']:
                    if cont['modelName'] == 'AlbumStore':
                        data.append({'des':cont['album']['fields']['description'],
                                     'name':cont['album']['fields']['name'],
                                     'cover':cont['album']['fields']['cover_image'],
                                     'id':cont['id']})
            else:
                more_data = False
    return jsonify(data)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8080, debug=True)
