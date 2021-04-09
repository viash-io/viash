#!/bin/bash

echo "Content from input1" > $par_output
echo "===================" >> $par_output
cat $par_input1 >> $par_output
echo "Content from input2" >> $par_output
echo "===================" >> $par_output
cat $par_input2 >> $par_output
