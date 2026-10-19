"""Generate the bathroom style-set assets: five bathtubs, five showers and the deluxe rubber duck.

Run from the project root: python tools/generate_bathroom_styles.py [--materials default|all|a,b]
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from bathroom_styles.generate import main  # noqa: E402

if __name__ == '__main__':
    main()
