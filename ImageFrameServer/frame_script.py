#!/usr/bin/python
# -*- coding:utf-8 -*-

import sys
import os
picdir = os.path.join(os.path.dirname(os.path.dirname(os.path.realpath(__file__))), 'pic')
libdir = os.path.join(os.path.dirname(os.path.dirname(os.path.realpath(__file__))), 'lib')

import time

from PIL import Image
from PIL import ImageDraw
from PIL import ImageFont
from PIL import ImageColor

from PIL import Image
from io import BytesIO

import json
import requests

assetId = sys.argv[1]

image_url = f"https://server_url/{assetId}/original"
api_token = ""

headers = {
    "x-api-key": f"{api_token}"
}

try:
    response = requests.get(image_url, headers = headers)

    if response.status_code == 200:  # Check if request was successful
        img = Image.open(BytesIO(response.content))  # Open image from bytes
        width, height = img.size  # Get image dimensions
    

        # Resize based on the height to maintain aspect ratio
        if width > height:
            new_height = 1200
            new_width = int((new_height / height) * width)
            img = img.resize((new_width, new_height))

            # Create a white background with the target size
            background = Image.new("RGB", (1600, 1200), (255, 255, 255))
            # Calculate the position to paste the resized image (centered vertically)
            paste_y = (1200 - new_height) // 2
            background.paste(img, (0, paste_y))
            background = background.rotate(-90, expand=True) 

        # If the image is taller than wider, resize based on the width
        else:
            new_width = 1200
            new_height = int((new_width / width) * height)
            img = img.resize((new_width, new_height))

            # Create a white background with the target size
            background = Image.new("RGB", (1200, 1600), (255, 255, 255))
            # Calculate the position to paste the resized image (centered horizontally)
            paste_y = (1600 - new_height) // 2
            background.paste(img, (0, paste_y))

        # Display the final image (with cropping and padding applied)
        #background.show()
    print(response.status_code)
    print("Script executed successfully")
    # epd.sleep()
except Exception as e:
    print(f"exept {e}")
    # epd.sleep()