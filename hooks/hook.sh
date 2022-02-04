# your_name="runoob"
# str="Hello, I know you are \"$your_name\"! \n"
# echo -e $str = "ZZ" 
currentPath=$PWD
folder="../.git/hooks/";
file="../.git/hooks/commit-msg"
if [ ! -x "$folder" ]; then
   mkdir "$folder"
fi

if [ -f "$file" ]; then
   rm -rf $file
fi
ln commit-msg ../.git/hooks/commit-msg