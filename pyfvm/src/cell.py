from collections import namedtuple


class Vec2(namedtuple('Vec2', ['x', 'y'])):
    def __add__(self, other):
        return Vec2(self.x + other.x, self.y + other.y)

    def __mul__(self, other):
        return Vec2(self.x * other, self.y * other)


class Face(namedtuple('Face', ['a', 'b'])):
    @property
    def center(self):
        return (self.a + self.b) * 0.5


class Cell:
    def __init__(self, faces: [Face]):
        self.faces = faces
        self.p = 0.0
        self.u_x = 0.0
        self.u_y = 0.0


class Grid:
    def __init__(self, cells: [[Cell]]):
        self.cells = cells

    def __str__(self):
        return str(cells)


cells = []
for i in range(10):
    row = []
    for j in range(10):
        faces: [Face] = [
            Face(Vec2(i, j), Vec2(i + 1, j)),
            Face(Vec2(i, j), Vec2(i, j + 1)),
            Face(Vec2(i + 1, j), Vec2(i + 1, j + 1)),
            Face(Vec2(i, j + 1), Vec2(i + 1, j + 1))
        ]
        cell: Cell = Cell(faces)
        row.append(cell)
    cells.append(row)

grid = Grid(cells)

dt = 0.01


