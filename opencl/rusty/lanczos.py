from cmath import sin, sqrt, pi, exp

p = [676.5203681218851
    ,-1259.1392167224028
    ,771.32342877765313
    ,-176.61502916214059
    ,12.507343278686905
    ,-0.13857109526572012
    ,9.9843695780195716e-6
    ,1.5056327351493116e-7
    ]

EPSILON = 1e-07
def drop_imag(z):
    if abs(z.imag) <= EPSILON:
        z = z.real
    return z

def gamma(z):
    z = complex(z)
    if z.real < 0.5:
        y = pi / (sin(pi*z) * gamma(1-z)) ### Reflection formula
    else:
        z -= 1
        x = 0.99999999999980993
        for (i, pval) in enumerate(p):
            x += pval / (z+i+1)
            print(x)
        t = z + len(p) - 0.5
        print(t)
        y = sqrt(2*pi) * t**(z+0.5) * exp(-t) * x
    return drop_imag(y)
"""
The above use of the reflection (thus the if-else structure) seems unnecessary
and just adds more code to execute. It calls itself again, so it still needs
to execute the same "for" loop yet has an extra calculation at the end)
"""

print gamma(5)
