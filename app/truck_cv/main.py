import cv2
img = cv2.imread('/Users/cielsealspheal/better-deerhack-scoop-scouter/DeerHacks-2023-Scoop-Scouter/app/truck_cv/cv_integration/include/ice-cream-truck-raw/16.jpg')
classifier = cv2.CascadeClassifier('/Users/cielsealspheal/better-deerhack-scoop-scouter/DeerHacks-2023-Scoop-Scouter/app/truck_cv/haarcascade_truck.xml')

# Convert the image to grayscale
gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

# Detect vehicles in the image
vehicles = classifier.detectMultiScale(gray, scaleFactor=1.1, minNeighbors=5)

# Check if the list of detected vehicles is not empty
if len(vehicles) > 0:
    print('The image contains a vehicle.')
else:
    print('The image does not contain a vehicle.')

# Draw rectangles around the detected vehicles
for (x, y, w, h) in vehicles:
    cv2.rectangle(img, (x, y), (x+w, y+h), (0, 255, 0), 2)

# Display the classified image
cv2.imshow('Classified Image', img)
cv2.waitKey(0)
cv2.destroyAllWindows()