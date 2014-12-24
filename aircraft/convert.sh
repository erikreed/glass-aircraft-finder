for i in *.PNG; do
    echo converting $i
    convert $i -monochrome -negate -transparent black -trim -resize 40x40 small/$i
done
