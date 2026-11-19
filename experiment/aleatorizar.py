"""Orden aleatorio reproducible de los microservicios dentro de cada sesión (sección 3.4.3).

Uso: python aleatorizar.py   (no requiere dependencias; la semilla queda registrada aquí y en asignacion.md)
"""
import random

SEMILLA = 20261128
GRUPO_A = ["P2", "P3", "P5"]  # sesión 1: manual    | sesión 2: PactGen-AI
GRUPO_B = ["P1", "P4"]        # sesión 1: PactGen-AI | sesión 2: manual

def condicion(p, sesion):
    manual_primero = p in GRUPO_A
    return ("manual" if manual_primero else "pactgen") if sesion == 1 else ("pactgen" if manual_primero else "manual")

rng = random.Random(SEMILLA)
for sesion in (1, 2):
    orden = GRUPO_A + GRUPO_B
    rng.shuffle(orden)
    print(f"Sesión {sesion}: " + ", ".join(f"{p} ({condicion(p, sesion)})" for p in orden))
