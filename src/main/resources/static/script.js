// Enable dragging and rotation for puzzle images
interact('.puzzle-image')
    .draggable({
        listeners: {
            move(event) {
                const { dx, dy } = event;
                const target = event.target;

                const x = (parseFloat(target.getAttribute('data-x')) || 0) + dx;
                const y = (parseFloat(target.getAttribute('data-y')) || 0) + dy;

                const currentRotation = parseFloat(target.getAttribute('data-rotation')) || 0;
                target.style.transform = `translate(${x}px, ${y}px) rotate(${currentRotation}deg)`;
                target.setAttribute('data-x', x);
                target.setAttribute('data-y', y);
            },
            end(event) {
                const target = event.target;
                const currentRotation = parseFloat(target.getAttribute('data-rotation')) || 0;
                const x = parseFloat(target.getAttribute('data-x')) || 0;
                const y = parseFloat(target.getAttribute('data-y')) || 0;

                target.style.transform = `translate(${x}px, ${y}px) rotate(${currentRotation}deg)`;
            }
        }
    })
    .on('tap', function (event) {
        const target = event.target;

        const x = parseFloat(target.getAttribute('data-x')) || 0;
        const y = parseFloat(target.getAttribute('data-y')) || 0;

        const currentRotation = parseFloat(target.getAttribute('data-rotation')) || 0;
        const newRotation = currentRotation + 90;
        target.style.transform = `translate(${x}px, ${y}px) rotate(${newRotation}deg)`;
        target.setAttribute('data-rotation', newRotation);
    });

function submitImageInfo() {
    const images = document.querySelectorAll('.puzzle-image');
    const imageInfo = [];

    for (const image of images) {
        const position = image.getBoundingClientRect();

        const startRotation = parseFloat(image.getAttribute('data-start-rotation')) || 0;
        const currentRotation = parseFloat(image.getAttribute('data-rotation')) || 0;

        const rotation = calculateRotation(startRotation, currentRotation);

        const info = {
            x: position.left,
            y: position.top,
            rotation: rotation,
            path: image.getAttribute('src'),
        };

        imageInfo.push(info);
    }

    function calculateRotation(startRotation, currentRotation) {
        const totalRotation = startRotation + currentRotation;
        const moduloRotation = totalRotation % 360;

        let rotation;
        if (moduloRotation < 0) {
            rotation = 360 + moduloRotation;
        } else {
            rotation = moduloRotation;
        }

        return rotation;
    }

    // Send the imageInfo array to the server
    $.ajax({
        url: '/submit',
        type: 'POST',
        data: JSON.stringify(imageInfo),
        contentType: 'application/json',
        success: function (response) {
            // Handle success response from the server
            if (response === true) {
                $('#message').text('Puzzle completed').removeClass('error').addClass('success');
            } else {
                $('#message').text('Try once more').removeClass('success').addClass('error');
            }
        },
        error: function () {
            // Handle error response from the server
            $('#message').text('Error').removeClass('success').addClass('error');
        }
    });
}