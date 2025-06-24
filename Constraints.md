
# variables
## Demands d_i
## Resources r_i
## Qualification score: 
For each r_i, and d_j we compute a score s_ij : 
0, if resource r_i does not satisfy demand d_j,
1, for perfect match, 
1 + number of over-qualification.

## Excluding Demands ex_i_j. 
compute exclusions! 
Sort demands over start time: collect exclusions d_i,d_j. 
D = 8
ex_i_j := d_i.start < d_j.start and d_i.end + 2*D > d_j.start

interval matching. 
symmetric. 

## Assignment  
A_ri_dj is the assignment of resource r_i to d_i
A_ri_dj \in \{0,1\}

\sum_{d_j in exclusions of d_i} A_ri_d_j <= 1

## Stability 
For all two consecutive demands by r_i, give a penalty for unequal construction side.

## Stay in team. 
Give a penalty in for each for 

# objective function

# constraints

